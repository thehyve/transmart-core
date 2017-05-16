package org.transmartproject.browse.fm

import annotation.*
import com.recomdata.util.FolderType
import grails.util.Holders
import grails.validation.ValidationException
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.transmart.biomart.BioData
import org.transmart.biomart.Experiment
import org.transmart.searchapp.AccessLog
import org.transmart.searchapp.AuthUser

class FmFolderService {

    final private static DEFAULT_FILE_TYPES =
            'xml,json,csv,pdf,doc,docx,ppt,pptx,xls,xlsx,odt,odp,ods,ott,otp,ots,rtf,htm,html,txt,log'

    static transactional = true

    def config = Holders.config
    def importDirectory = config.com.recomdata.FmFolderService.importDirectory
    def filestoreDirectory = config.com.recomdata.FmFolderService.filestoreDirectory
    def fileTypes = config.com.recomdata.FmFolderService.fileTypes ?: DEFAULT_FILE_TYPES
    String solrBaseUrl = config.com.recomdata.solr.baseURL
    def amTagItemService
    def springSecurityService
    def i2b2HelperService
    def facetsIndexingService

    private String getSolrUrl() {
        solrBaseUrl + '/update'
    }

    /**
     * Imports files processing them into filestore and indexing them with SOLR.
     *
     * @return
     */
    def importFiles() {

        log.info "importFiles() called"

        log.debug "Importing files from $importDirectory into $filestoreDirectory"

        if (!importDirectory || !filestoreDirectory || !solrUrl) {
            if (!importDirectory) {
                log.error "Unable to check for new files. " +
                        "com.recomdata.FmFolderService.importDirectory setting " +
                        "has not been defined in the Config.groovy file"
            }
            if (!filestoreDirectory) {
                log.error "Unable to check for new files. " +
                        "com.recomdata.FmFolderService.filestoreDirectory " +
                        "setting has not been defined in the Config.groovy file"
            }
            if (!solrUrl) {
                log.error "Unable to check for new files. " +
                        "com.recomdata.solr.baseURL " +
                        "setting has not been defined in the Config.groovy file"
            }
            return;
        }

        processDirectory new File(importDirectory)

        log.debug "Finished importing files"
    }

    /**
     * Re-index all files through SOLR.
     * @return
     */
    def reindexFiles() {

        log.info("reindexFiles called");
        def fmFiles = FmFile.findAll();
        for (fmFile in fmFiles) {
            indexFile(fmFile);
        }

    }

    /**
     * Process files and sub-directories in specified directory.
     *
     * @param directory
     * @return
     */
    def processDirectory(File directory) {
        def fmFolder = null
        /* null: uninitialized; false: not a folder it */

        /* the lazy initialization is to avoid loading the fmFolder if there are
         * actually no files under the directory being processed */
        def getFmFolder = { ->
            if (fmFolder != null) {
                return fmFolder
            }

            long folderId
            try {
                folderId = Long.parseLong directory.name
            } catch (NumberFormatException ex) {
                fmFolder = false
                return fmFolder
            }

            fmFolder = FmFolder.get folderId
            if (fmFolder == null) {
                log.error "Folder with id $folderId does not exist (reference " +
                        "in directory $directory)"
                fmFolder = false
            }

            fmFolder
        }

        log.debug "Looking for data in $directory"

        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                processDirectory(file);
            } else if (file.name != '.keep') {
                if (getFmFolder()) {
                    processFile getFmFolder(), file
                } else {
                    log.warn "Ignoring file $file because its parent " +
                            "directory $directory could not be matched to a " +
                            "folder in tranSMART"
                }
            }
        }

        if (directory != new File(importDirectory) /* not import root */ &&
                directory.list().length == 0) {
            if (!directory.delete()) {
                log.warn "Could not delete presumably empty directory $directory"
            } else {
                log.debug "Deleted empty directory $directory"
            }
        }

    }

    /**
     * Processes a file into the filestore associating it with a folder and
     * indexes file using SOLR
     *
     * @param file file to be proceessed
     * @return
     */
    private void processFile(FmFolder fmFolder, File file, String customName = null, String description = null) {
        log.info "Importing file $file into folder $fmFolder"

        // Check if folder already contains file with same name.
        def fmFile;

        def newFileName = customName?:file.getName()

        for (f in fmFolder.fmFiles) {
            if (f.originalName == newFileName) {
                fmFile = f;
                break;
            }
        }
        // If it does, then use existing file record and increment its version.
        // Otherwise, create a new file.
        if (fmFile != null) {
            fmFile.fileVersion++;
            fmFile.fileSize = file.length();
            fmFile.linkUrl = "";
            log.info("File = " + file.getName() + " (" + fmFile.id + ") - Existing");
        } else {
            fmFile = new FmFile(
                    displayName: newFileName,
                    originalName: file.getName(),
                    fileType: getFileType(file),
                    fileSize: file.length(),
                    filestoreLocation: "",
                    filestoreName: "",
                    linkUrl: "",
                    fileDescription: description
            );
            if (!fmFile.save(flush: true)) {
                fmFile.errors.each {
                    log.error(it);
                }
                return;
            }
            fmFile.filestoreLocation = getFilestoreLocation(fmFolder);
            fmFolder.addToFmFiles(fmFile);
            if (!fmFolder.save(flush: true)) {
                fmFolder.errors.each {
                    log.error(it);
                }
                return;
            }
            log.info("File = " + file.getName() + " (" + fmFile.id + ") - New");
        }

        fmFile.filestoreName = fmFile.id + "-" + fmFile.fileVersion + "." + fmFile.fileType;

        if (!fmFile.save(flush: true)) {
            fmFile.errors.each {
                log.error(it);
            }
            return;
        }

        // Use filestore directory based on file's parent study or common directory
        // for files in folders above studies. If directory does not exist, then create it.
        // PREREQUISITE: Service account running tomcat has ownership of filestore directory.
        File filestoreDir = new File(filestoreDirectory + fmFile.filestoreLocation);
        if (!filestoreDir.exists()) {
            if (!filestoreDir.mkdirs()) {
                log.error("unable to create filestore " + filestoreDir.getPath());
                return;
            }
        }

        // Move file to appropriate filestore directory.
        File filestoreFile = new File(filestoreDirectory + fmFile.filestoreLocation + file.separator + fmFile.filestoreName);
        try {
            FileUtils.copyFile(file, filestoreFile);
            if (!file.delete()) {
                log.error("unable to delete file " + file.getPath());
            }
            //if (!file.renameTo(filestoreFile)) {
        } catch (IOException ex) {
            log.error("unable to copy file to " + filestoreFile.getPath());
            return;
        }

        log.info("Moved file to " + filestoreFile.getPath());

        // Call file indexer.
        indexFile(fmFile);

    }

    /**
     * Gets type (extension) of specified file or an empty string if it cannot
     * be determined
     *
     * @param file
     * @return
     */
    private String getFileType(File file) {
        FilenameUtils.getExtension file.getName()
    }

    /**
     * Gets filestore location for specified folder. Files are stored in directories
     * grouped by their parent study folder id. If the files are being loaded at
     * the program level, then a default folder, "0" will be used.
     *
     * @param folder
     * @return
     */
    private String getFilestoreLocation(FmFolder fmFolder) {

        String filestoreLocation

        if (fmFolder.folderLevel == 0) {
            filestoreLocation = "0"
        } else if (fmFolder.folderLevel == 1) {
            filestoreLocation = fmFolder.id
        } else {
            log.debug("folderFullName = ${fmFolder.folderFullName}")
            int pos = fmFolder.folderFullName.indexOf("\\", 1)
            pos = fmFolder.folderFullName.indexOf("\\", pos + 1)
            log.debug("find name = ${fmFolder.folderFullName.substring(0, pos)}")
            FmFolder fmParentFolder = FmFolder.findByFolderFullName(fmFolder.folderFullName.substring(0, pos + 1))
            if (fmParentFolder == null) {
                log.error("Unable to find folder with folderFullName of " + fmFolder.folderFullName.substring(0, pos + 1))
                filestoreLocation = "0"
            } else {
                filestoreLocation = fmParentFolder.id
            }
        }

        return File.separator + filestoreLocation

    }

    /**
     * Indexes file using SOLR.
     * @param fileId ID of file to be indexed
     * @return
     */
    private void indexFile(String fileId) {

        FmFile fmFile = FmFile.get(fileId);
        if (fmFile == null) {
            log.error("Unable to locate fmFile with id of " + fileId);
            return;
        }
        indexFile(fmFile);

    }

    /**
     * Indexes file using SOLR.
     * @param fmFile file to be indexed
     * @return
     */
    private void indexFile(FmFile fmFile) {
        try {
            facetsIndexingService.indexByIds([this.getClass()
                                                      .classLoader.loadClass('org.transmartproject.search.indexing.FacetsDocId')
                                                      .newInstance('FILE', fmFile.id)] as Set)
        } catch (Exception ex) {
            log.error("Exception while indexing fmFile with id of " + fmFile.id, ex);
        }
    }

    /**
     * Removes files/folders by UID from the SOLR index.
     * @param uid UID of file or folder to remove
     * @return
     */
    def removeSolrEntry(String uid) {

        try {

            String xmlString = "<delete><query>id:\"" + uid + "\"</query></delete>"
            xmlString = URLEncoder.encode(xmlString, "UTF-8");
            StringBuilder url = new StringBuilder(solrUrl);
            url.append("?stream.body=").append(xmlString).append("&commit=true")
            URL updateUrl = new URL(url.toString())
            HttpURLConnection urlc = (HttpURLConnection) updateUrl.openConnection();
            if (HttpURLConnection.HTTP_OK != urlc.getResponseCode()) {
                log.warn("The SOLR service returned an error #" + urlc.getResponseCode() + " " + urlc.getResponseMessage() + " for url " + updateUrl);
            }
        } catch (Exception ex) {
            log.error("Exception while deleting entry with uid of " + uid, ex);
        }

    }

    def deleteFolder(FmFolder folder) {
        //Delete all files within this folder.
        //Convert PersistentSets to Arrays to avoid concurrent modification
        def files = folder.fmFiles.toArray()
        for (file in files) {
            deleteFile(file)
        }
        def children = folder.children.toArray()
        for (child in children) {
            deleteFolder(child)
        }
        folder.activeInd = false
        removeSolrEntry(folder.getUniqueId())
        if (!folder.save(flush: true)) {
            log.error("Unable to delete folder with uid of " + folder.getUniqueId());
        } else {
            def al = new AccessLog(username: springSecurityService.getPrincipal().username, event: "Browse-Delete object", eventmessage: folder.folderType + ": " + folder.folderName + " (" + folder.getUniqueId() + ")", accesstime: new java.util.Date())
            al.save()
        }
    }

    def getFile(FmFile file) {
        new File(
                new File(filestoreDirectory, file.filestoreLocation),
                file.filestoreName)
    }

    def deleteFile(FmFile file) {
        try {
            File filestoreFile = getFile(file)
            if (filestoreFile.exists()) {
                filestoreFile.delete()
            }
            removeSolrEntry(file.getUniqueId())
            def data = FmData.get(file.id)
            if (data) {
                data.delete(flush: true)
            }
            file.folder.fmFiles.remove(file)
            file.folder.save(flush: true)
            def al = new AccessLog(username: springSecurityService.getPrincipal().username, event: "Browse-Delete file", eventmessage: file.displayName + " (" + file.getUniqueId() + ")", accesstime: new java.util.Date())
            al.save()
        }
        catch (Exception ex) {
            System.out.println("Exception while deleting file with uid of " + file.getUniqueId(), ex);
            log.error("Exception while deleting file with uid of " + file.getUniqueId(), ex);
        }
    }

    List<FmFolder> getFolderContents(id) {
        def parent
        def folderLevel = 0L;
        if (id) {
            parent = FmFolder.get(id)
            folderLevel = parent.folderLevel + 1
        }

        FmFolder.createCriteria().list {
            if (parent != null) {
                eq('parent', parent)
            }
            eq('folderLevel', folderLevel)
            eq('activeInd', true)
            order('folderName', 'asc')
        }
    }

    Map<FmFolder, String> getAccessLevelInfoForFolders(AuthUser user, Collection<FmFolder> fmFolders) {
        if (!fmFolders) return [:]

        boolean isAdmin = user && (user.isAdmin() || user.isDseAdmin())

        def foldersByStudy = fmFolders.groupBy { it.findParentStudyFolder() }

        def userAssignedTokens, studyFolderStudyIdMap, studyTokensMap
        if (!isAdmin) {
            studyFolderStudyIdMap = foldersByStudy.keySet().findAll().collectEntries {
                def studyId = FmFolderAssociation.findByFmFolder(it)?.getBioObject()?.accession
                [(it): studyId]
            }

            studyTokensMap = i2b2HelperService.getSecureTokensForStudies(studyFolderStudyIdMap.values().findAll())

            userAssignedTokens = i2b2HelperService.getSecureTokensWithAccessForUser(user)
        }

        def results = [:]
        foldersByStudy.each { FmFolder studyFolder, List<FmFolder> folders ->
            if (studyFolder) {
                if (isAdmin) {
                    results += folders.collectEntries { [(it): 'ADMIN'] }
                } else {
                    def studyId = studyFolderStudyIdMap[studyFolder]
                    def token = studyTokensMap[studyId]
                    def accessLevelInfo = userAssignedTokens[token] ?: 'LOCKED'
                    results += folders.collectEntries { [(it): accessLevelInfo] }
                }
            } else {
                results += folders.collectEntries { [(it): 'NA'] }
            }
        }

        results
    }

    String getAccessLevelInfoForFolder(AuthUser user, FmFolder fmFolder) {
        def map = getAccessLevelInfoForFolders(user, [fmFolder])
        map ? map.values()[0] : null
    }

    Map<FmFolder, String> getFolderContentsWithAccessLevelInfo(AuthUser user, folderId) {
        def childFolders = getFolderContents(folderId)
        getAccessLevelInfoForFolders(user, childFolders)
    }

    def getAssociatedAccession(fmFolder) {
        //Walk up the tree to find the study accession for this folder
        if (!fmFolder) {
            return null
        }

        if (fmFolder.folderType.equals(FolderType.PROGRAM.name())) {
            //Programs use their folderUID as accession
            return fmFolder.getUniqueId()
        }

        if (fmFolder.folderType.equals(FolderType.STUDY.name())) {
            def experiment = FmFolderAssociation.findByFmFolder(fmFolder)?.getBioObject()
            if (!experiment) {
                log.error("No experiment associated with study folder: " + fmFolder.folderFullName)
            }
            return experiment?.accession
        } else {
            return getAssociatedAccession(fmFolder.parent)
        }
    }

    def getPath(fmFolder, safe = false) {
        //Get the full path of a folder by gathering folder names
        def names = [fmFolder.folderName]
        while (fmFolder.parent != null) {
            fmFolder = fmFolder.parent
            names.add(0, fmFolder.folderName)
        }

        if (safe) {
            for (int i = 0; i < names.size(); i++) {
                names[i] = safeFileName(names[i])
            }
        }

        def path = names.join("/")
        return path
    }

    def filenameBlacklist = "\\/:;*?\"<>|"

    def safeFileName(name) {
        //Handle special cases - files should not be named like this!
        if (name.equals(".")) {
            return "dot"
        }
        if (name.equals("..")) {
            return "dotdot"
        }
        //Normal sanitation, should cover Windows/Unix
        for (chr in filenameBlacklist) {
            name = name.replace(chr, "_")
        }
        return name
    }

    /**
     * Validates and saves folder, associated business, and metadata fields.
     * @param folder
     * @param object associated business object or folder, if there is none
     * @param values params
     * @throws ValidationException if required fields are missing or error saving to database
     */
    private void saveFolder(FmFolder folder, Object object, Map values) {

        AmTagTemplate template = AmTagTemplate.findByTagTemplateType(folder.folderType)

        // If this is new folder, then use viewInGrid items for validation, otherwise use editable items.
        def sql
        if (folder.id == null) {
            sql = "from AmTagItem ati where ati.amTagTemplate.id = :templateId and ati.viewInGrid = 1 order by displayOrder"
        } else {
            sql = "from AmTagItem ati where ati.amTagTemplate.id = :templateId and ati.editable = 1 order by displayOrder"
        }
        List items = AmTagItem.findAll(sql, [templateId: template.id])

        validateFolder(folder, object, items, values);
        doSaveFolder(folder, object, template, items, values);

    }

    /**
     * Validates required folder and meta data fields.
     * @param folder
     * @param object
     * @param items
     * @param values
     * @throws ValidationException
     */
    private void validateFolder(FmFolder folder, Object object, List items, Map values) {

        // Validate folder specific fields, if there is no business object
        if (folder == object) {
            List fields = [[displayName: "Name", fieldName: "folderName"],
                           [displayName: "Description", fieldName: "description"]]
            for (field in fields) {
                def value = values[field.fieldName]
                if (value == null || value.length() == 0) {
                    folder.errors.rejectValue(field.fieldName, "blank", [field.displayName] as String[], "{0} field requires a value.")
                }
            }
        }

        for (item in items) {
            if (item.required) {
                def value = null;
                if (item.tagItemType.equals("FIXED")) {
                    value = values.list(item.tagItemAttr)
                    //					log.info "validate item.tagItemAtrr = ${item.tagItemAttr}"
                } else {
                    value = values.list("amTagItem_" + item.id)
                    //					log.info "validate item.tagItemAtrr = amTagItem_${item.id}"
                }
                //				log.info "validate value = ${value}, value.size() = ${value.size()}"
                if (value.size() == 0 || value[0] == null || value[0].length() == 0) {
                    folder.errors.rejectValue("id", "blank", [item.displayName] as String[], "{0} field requires a value.")
                }
                // TODO: Check for max values
            } else {
                log.info item.displayName + " not required"
            }

            //check for unique study identifer
            if (item.codeTypeName == 'STUDY_IDENTIFIER') {
                if (Experiment.findByAccession(values.list(item.tagItemAttr))) {
                    folder.errors.rejectValue("id", "blank", [item.displayName] as String[], "{0} must be unique.")
                }
            }
        }

        if (folder.hasErrors()) {
            throw new ValidationException("Validation errors occurred.", folder.errors)
        }

    }

    /**
     * Saves folder, associated business object, and metadata fields
     * @param folder folder to be saved
     * @param object associated business object or folder, if there is none
     * @param template tag template associated with folder
     * @param items items associated with template
     * @param values field values to be saved
     * @throws ValidationException if there are any errors persisting data to the database
     */
    private void doSaveFolder(FmFolder folder, Object object, AmTagTemplate template, List items, Map values) {

        // Save folder object
        folder.save(flush: true, failOnError: true)

        // Using items associated with this folder's template, set business object property values or create tags.
        for (tagItem in items) {
            def newValue = null
            if (tagItem.tagItemType.equals('FIXED')) {
                newValue = values."${tagItem.tagItemAttr}"
                if (newValue != null) {
                    def value = ""
                    if (tagItem.tagItemSubtype.equals('MULTIPICKLIST')) {
                        newValue = values.list("${tagItem.tagItemAttr}")
                        if (newValue != null && newValue != "" && newValue.size() > 0) {
                            newValue.each {
                                if (value != "") {
                                    value += "|"
                                }
                                value += it
                            }
                        }
                    } else {
                        value = newValue
                    }
                    object."${tagItem.tagItemAttr}" = value
                }
            } else if (tagItem.tagItemType.equals('CUSTOM')) {
                newValue = values."amTagItem_${tagItem.id}"
                if (tagItem.tagItemSubtype.equals('FREETEXT') || tagItem.tagItemSubtype.equals('FREETEXTAREA')) {
                    AmTagAssociation.executeUpdate("delete from AmTagAssociation as ata where ata.objectType=:objectType and ata.subjectUid=:subjectUid and ata.tagItemId=:tagItemId", [objectType: "AM_TAG_VALUE", subjectUid: folder.getUniqueId(), tagItemId: tagItem.id])
                    if (newValue != null && newValue != "") {
                        AmTagValue newTagValue = new AmTagValue(value: newValue)
                        newTagValue.save(flush: true, failOnError: true)
                        AmTagAssociation tagAssoc = new AmTagAssociation(objectType: 'AM_TAG_VALUE', subjectUid: folder.getUniqueId(), objectUid: newTagValue.getUniqueId(), tagItemId: tagItem.id)
                        tagAssoc.save(flush: true, failOnError: true)
                    }
                } else if (tagItem.tagItemSubtype.equals('PICKLIST')) {
                    AmTagAssociation.executeUpdate("delete from AmTagAssociation as ata where ata.objectType=:objectType and ata.subjectUid=:subjectUid and ata.tagItemId=:tagItemId", [objectType: "BIO_CONCEPT_CODE", subjectUid: folder.getUniqueId(), tagItemId: tagItem.id])
                    if (newValue != null && newValue != "") {
                        AmTagAssociation tagAssoc = new AmTagAssociation(objectType: 'BIO_CONCEPT_CODE', subjectUid: folder.getUniqueId(), objectUid: newValue, tagItemId: tagItem.id)
                        tagAssoc.save(flush: true, failOnError: true)
                    }
                } else if (tagItem.tagItemSubtype.equals('MULTIPICKLIST')) {
                    AmTagAssociation.executeUpdate("delete from AmTagAssociation as ata where ata.objectType=:objectType and ata.subjectUid=:subjectUid and ata.tagItemId=:tagItemId", [objectType: "BIO_CONCEPT_CODE", subjectUid: folder.getUniqueId(), tagItemId: tagItem.id])
                    newValue = values.list("amTagItem_${tagItem.id}")
                    if (newValue != null && newValue != "" && newValue.size() > 0) {
                        newValue.each {
                            if (it) {
                                AmTagAssociation tagAssoc = new AmTagAssociation(objectType: 'BIO_CONCEPT_CODE', subjectUid: folder.getUniqueId(), objectUid: it, tagItemId: tagItem.id)
                                tagAssoc.save(flush: true, failOnError: true)
                            } else {
                                log.error("amTagItem_${tagItem.id} is null")
                            }
                        }
                    }
                }
            } else {
                newValue = values.list("amTagItem_${tagItem.id}")
                AmTagAssociation.executeUpdate("delete from AmTagAssociation as ata where ata.objectType=:objectType and ata.subjectUid=:subjectUid and ata.tagItemId=:tagItemId", [objectType: tagItem.tagItemType, subjectUid: folder.getUniqueId(), tagItemId: tagItem.id])
                if (newValue != null && newValue != "" && newValue.size() > 0) {
                    newValue.each {
                        if (it) {
                            AmTagAssociation tagAssoc = new AmTagAssociation(objectType: tagItem.tagItemType, subjectUid: folder.getUniqueId(), objectUid: it, tagItemId: tagItem.id)
                            tagAssoc.save(flush: true, failOnError: true)
                        } else {
                            log.error("amTagItem_${tagItem.id} is null")
                        }
                    }
                }
            }

        }

        // Create tag template association between folder and template, if it does not already exist
        AmTagTemplateAssociation templateAssoc = AmTagTemplateAssociation.findByObjectUid(folder.getUniqueId())
        if (templateAssoc == null) {
            templateAssoc = new AmTagTemplateAssociation(tagTemplateId: template.id, objectUid: folder.getUniqueId())
            templateAssoc.save(flush: true, failOnError: true)
            def al = new AccessLog(username: springSecurityService.getPrincipal().username, event: "Browse-Create object", eventmessage: folder.folderType + ": " + folder.folderName + " (" + folder.getUniqueId() + ")", accesstime: new java.util.Date())
            al.save()
        } else {
            def al = new AccessLog(username: springSecurityService.getPrincipal().username, event: "Browse-Modify object", eventmessage: folder.folderType + ": " + folder.folderName + " (" + folder.getUniqueId() + ")", accesstime: new java.util.Date())
            al.save()
        }

        // If there is business object associated with folder, then save it and create association, if it does not exist.
        if (object != folder) {
            //			log.info "FmFolderService.saveFolder object.properties = ${object.properties}"
            object.save(flush: true, failOnError: true)
            FmFolderAssociation folderAssoc = FmFolderAssociation.findByFmFolder(folder)
            if (folderAssoc == null) {
                BioData bioData = BioData.get(object.id)
                folderAssoc = new FmFolderAssociation(objectUid: bioData.uniqueId, objectType: object.getClass().getName(), fmFolder: folder)
                folderAssoc.save(flush: true, failOnError: true)
            }
        }
    }

    /**
     * Helper method to check whether a folder's parent program is included in the current search results.
     *
     * @param folderSearchString The current search string
     * @param folderFullName The folder's full path (from which the program UID can be extracted).
     * @return
     */
    def searchMatchesParentProgram(folderSearchString, folderFullName) {
        def paths = folderSearchString.split(",")
        def index = folderFullName.indexOf("\\", 1)
        def programUID = folderFullName.substring(1, index)
        return ("\\" + programUID + "\\" in paths)
    }

    def getFolderByBioDataObject(bioDataObject) {
        def uniqueId = bioDataObject?.getUniqueId()?.uniqueId
        if (!uniqueId) {
            log.error("No unique ID found for bio object " + bioDataObject?.id)
            return null
        }
        FmFolderAssociation ffa = FmFolderAssociation.findByObjectUid(uniqueId)
        if (!ffa) {
            log.error("No fmFolderAssociation found for unique ID " + uniqueId)
        }
        return ffa?.fmFolder
    }

    /**
     * @param parentId
     * @return list of folders which are the children of the folder of which the identifier is passed as parameter
     */
    List<FmFolder> getChildrenFolder(String parentId) {
        def folder = FmFolder.get(parentId)
        return FmFolder.executeQuery("from FmFolder as fd where fd.activeInd = true and fd.folderFullName like :fn and fd.folderLevel= :fl ", [fl: folder.folderLevel + 1, fn: folder.folderFullName + "%"])
    }

    /**
     * @param parentId
     * @return list of folders which are the children of the folder of which the identifier is passed as parameter by folder types
     */
    List<FmFolder> getChildrenFolderByType(Long parentId, String folderType) {
        def folder = FmFolder.get(parentId)
        return FmFolder.executeQuery("from FmFolder as fd where fd.activeInd = true and fd.folderFullName like :fn and fd.folderLevel= :fl and upper(fd.folderType) = upper(:ft)", [fl: folder.folderLevel + 1, fn: folder.folderFullName + "%", ft: folderType])
    }

    /**
     * @param parentId
     * @return list of folders which are the children of the folder of which the identifier is passed as parameter
     */
    List getChildrenFolderTypes(Long parentId) {
        def folder = FmFolder.get(parentId)
        return FmFolder.executeQuery("select distinct(fd.folderType) from FmFolder as fd where fd.activeInd = true and fd.folderFullName like :fn and fd.folderLevel= :fl ", [fl: folder.folderLevel + 1, fn: folder.folderFullName + "%"])
    }
}
