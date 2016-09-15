package fm

import annotation.AmTagAssociation
import annotation.AmTagTemplate
import annotation.AmTagValue
import org.transmart.biomart.BioAssayPlatform
import org.transmart.biomart.BioData
import org.transmart.biomart.ConceptCode
import org.transmart.biomart.Experiment
import org.transmart.searchapp.SearchKeyword

import fm.FmFile
import fm.FmFolder
import fm.FmFolderAssociation
import org.transmart.mongo.MongoUtils;

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import com.mongodb.Mongo
import com.mongodb.DB
import com.mongodb.MongoClient;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;

import groovyx.net.http.ContentType;
import groovyx.net.http.HTTPBuilder;
import groovyx.net.http.Method;

class FileExportController {

    def fmFolderService
    def amTagItemService

    def add = {
        def paramMap = params
        def idList = params.id.split(',')

        def exportList = session['foldermanagement.exportlist']

        if (exportList == null) {
            exportList = [];
        }
        for (id in idList) {
            if (id && !exportList.contains(id)) {
                exportList.push(id)
            }
        }
        session['foldermanagement.exportlist'] = exportList;

        //Render back the number to display
        render(status: 200, text: exportList.size())
    }

    def remove = {
        def idList = params.id.split(',')

        def exportList = session['foldermanagement.exportlist']

        if (exportList == null) {
            exportList = [];
        }
        for (id in idList) {
            if (id && exportList.contains(id)) {
                exportList.remove(id)
            }
        }
        session['foldermanagement.exportlist'] = exportList;

        //Render back the number to display
        render(status: 200, text: exportList.size())
    }

    def view = {

        def exportList = session['foldermanagement.exportlist']
        def files = []
        for (id in exportList) {
            FmFile f = FmFile.get(id)
            if (f) {
                files.push([id: f.id, fileType: f.fileType, displayName: f.displayName, folder: fmFolderService.getPath(f.folder)])
            }
        }
        files.sort { a, b ->
            if (!a.folder.equals(b.folder)) {
                return a.folder.compareTo(b.folder)
            }
            return a.displayName.compareTo(b.displayName)
        }

        render(template: 'export', model: [files: files], plugin: 'folderManagement')
    }

    def export = {

        def errorResponse = []
        def filestorePath = grailsApplication.config.com.recomdata.FmFolderService.filestoreDirectory

        def useMongo = grailsApplication.config.org.transmart.mongoFiles.enableMongo

        def exportList
        def metadataExported = new HashSet();
        try {

            //Final export list comes from selected checkboxes
            exportList = params.id.split(",")

            ByteArrayOutputStream baos = new ByteArrayOutputStream()
            def zipStream = new ZipOutputStream(baos)

            def manifestMap = [:]

            for (f in exportList) {
                FmFile fmFile = FmFile.get(f)
                def fileLocation = filestorePath + File.separator + fmFile.filestoreLocation + File.separator + fmFile.filestoreName
                File file = new File(fileLocation)
                if (file.exists()) {

                    //Construct a file name out of the display name + suffix, if needed
                    def exportName = fmFile.displayName;
                    if (!exportName.endsWith("." + fmFile.fileType)) {
                        exportName += "." + fmFile.fileType
                    }

                    String dirName = fmFolderService.getPath(fmFile.folder, true)
                    if (dirName.startsWith("/") || dirName.startsWith("\\")) {
                        dirName = dirName.substring(1)
                    }
                    //Lose the first separator character, this would cause a blank folder name in the zip
                    def fileEntry = new ZipEntry(dirName + "/" + fmFolderService.safeFileName(exportName))
                    zipStream.putNextEntry(fileEntry)
                    if(!useMongo){
                        if (file.exists()) {
                            file.withInputStream({ is -> zipStream << is })
                        } else {
                            def errorMessage = "File not found for export: " + fileLocation
                            log.error errorMessage
                            errorResponse += errorMessage
                        }
                    } else{
                        if(grailsApplication.config.org.transmart.mongoFiles.useDriver){
                            MongoClient mongo = new MongoClient(grailsApplication.config.org.transmart.mongoFiles.dbServer, grailsApplication.config.org.transmart.mongoFiles.dbPort)
                            DB db = mongo.getDB( grailsApplication.config.org.transmart.mongoFiles.dbName)
                            GridFS gfs = new GridFS(db)
                            GridFSDBFile gfsFile = gfs.findOne(fmFile.filestoreName)
                            if(gfsFile==null){
                                def errorMessage = "File not found for export: " + fileLocation
                                log.error errorMessage
                                errorResponse += errorMessage
                            }else{
                                zipStream << gfsFile.getInputStream()
                            }
                            mongo.close()
                        }else{
                            def apiURL = grailsApplication.config.org.transmart.mongoFiles.apiURL
                            def apiKey = grailsApplication.config.org.transmart.mongoFiles.apiKey
                            def http = new HTTPBuilder(apiURL+fmFile.filestoreName+"/fsfile")
                            http.request( Method.GET, ContentType.BINARY) { req ->
                                headers.'apikey' = MongoUtils.hash(apiKey)
                                response.success = { resp, binary ->
                                    assert resp.statusLine.statusCode == 200
                                    def inputStream = binary
                                    byte[] dataBlock = new byte[1024];
                                    int count = inputStream.read(dataBlock, 0, 1024);
                                    while (count != -1) {
                                        zipStream.write(dataBlock, 0, count);
                                        count = inputStream.read(dataBlock, 0, 1024);
                                    }
                                }
                                response.failure = { resp ->
                                    def errorMessage = "File not found for export: " + fmFile.filestoreName
                                    log.error("Problem during connection to API: "+resp.status)
                                    render(contentType: "text/plain", text: "Error writing ZIP: File not found")
                                }
                            }
                        }
                    }
                    zipStream.closeEntry()

                    //For manifest files, add this file to a map, keyed by folder names.
                    def manifestList = []
                    if (manifestMap.containsKey(dirName)) {
                        manifestList = manifestMap.get(dirName)
                    }

                    manifestList.push(fmFile)
                    manifestMap.put(dirName, manifestList)

                    //for each folder of the hierarchy of the file path, add file with metadata
                    def path = fmFile.folder.folderFullName
                    if (metadataExported.add(path)) exportMetadata(path, zipStream);

                } else {
                    def errorMessage = "File not found for export: " + fileLocation
                    log.error errorMessage
                    errorResponse += errorMessage
                }
            }
            //Now for each item in the manifest map, create a manifest file and add it to the ZIP.
            def keyset = manifestMap.keySet()
            for (key in keyset) {
                def manifestEntry = new ZipEntry(key + "/" + "manifest.txt")
                zipStream.putNextEntry(manifestEntry)
                def manifestList = manifestMap.get(key)
                zipStream.write((String.format("%60s%5s%15s\n", "File Name", "Type", "Size")).getBytes())
                zipStream.write("--------------------------------------------------------------------------------\n".getBytes())
                for (fmFileIt in manifestList) {
                    zipStream.write((String.format("%60s%5s%15d\n", fmFileIt.displayName, fmFileIt.fileType, fmFileIt.fileSize)).getBytes())
                }
                zipStream.closeEntry()
            }
            zipStream.flush();
            zipStream.close();

            response.setHeader('Content-disposition', 'attachment; filename=export.zip')
            response.contentType = 'application/zip'
            response.outputStream << baos.toByteArray()
            response.outputStream.flush()
        }
        catch (Exception e) {
            log.error("Error writing ZIP", e)
            render(contentType: "text/plain", text: errorResponse.join("\n") + "\nError writing ZIP: " + e.getMessage())
        } catch (OutOfMemoryError oe) {
            log.error("Files too large to be exported: " + exportList)
            render(contentType: "text/plain", text: "Error: Files too large to be exported.\nPlease click on the \"Previous\" button on your web browser to go back to tranSMART.")
        }
    }

    //add in a zip a file containing metadata for a given folder
    private void exportMetadata(String path, ZipOutputStream zipStream) {
        try {
            //create path for the metadata file
            def dirName = ""
            for (folderFullId in path.split("\\\\", -1)) {
                if (!folderFullId.equals("")) {
                    def folderId = folderFullId.split(":", 2)[1]
                    if (dirName.compareTo("") != 0) dirName += "/"
                    dirName += fmFolderService.safeFileName((FmFolder.get(folderId)).folderName)
                }
            }
            if (dirName.startsWith("/") || dirName.startsWith("\\")) {
                dirName = dirName.substring(1)
            } //Lose the first separator character, this would cause a blank folder name in the zip

            def fileEntry = new ZipEntry(dirName + "/metadata.txt")
            zipStream.putNextEntry(fileEntry)
            for (folderFullId in path.split("\\\\", -1)) {
                if (!folderFullId.equals("")) {
                    def folderId = folderFullId.split(":", 2)[1]
                    def folder = FmFolder.get(folderId)

                    def amTagTemplate = AmTagTemplate.findByTagTemplateType(folder.folderType)
                    def metaDataTagItems = amTagItemService.getDisplayItems(amTagTemplate.id)

                    zipStream.write((folder.folderType + ": " + folder.folderName + "\r\n").getBytes())
                    zipStream.write(("Description: " + (folder.description).replace("\n", " ") + "\r\n").getBytes())

                    //get associated bioDataObject
                    def bioDataObject
                    def folderAssociation = FmFolderAssociation.findByFmFolder(folder)
                    if (folderAssociation) {
                        bioDataObject = folderAssociation.getBioObject()
                    }
                    if (!bioDataObject) {
                        bioDataObject = folder
                    }

                    for (amTagItem in metaDataTagItems) {
                        if (amTagItem.tagItemType == 'FIXED') {
                            if (amTagItem.tagItemAttr != null ? bioDataObject?.hasProperty(amTagItem.tagItemAttr) : false) {
                                def values = ""
                                def value = fieldValue(bean: bioDataObject, field: amTagItem.tagItemAttr)
                                for (v in (value.split("\\|", -1))) {
                                    def bioData = BioData.findByUniqueId(v)
                                    if (bioData != null) {
                                        def concept = ConceptCode.findById(bioData.id)
                                        if (concept != null) {
                                            if (values != "") values += "; "
                                            values += concept.codeName
                                        }
                                    }
                                }
                                if (values.compareTo("") == 0 && value != null) values = value;
                                zipStream.write((amTagItem.displayName + ": " + values + "\r\n").getBytes())
                            }
                        } else if (amTagItem.tagItemType == 'CUSTOM') {
                            if (amTagItem.tagItemSubtype == 'FREETEXT') {
                                def value = ""
                                def tagAssoc = AmTagAssociation.find("from AmTagAssociation where subjectUid=? and tagItemId=?", ["FOL:" + folderId, amTagItem.id])
                                if (tagAssoc != null) {
                                    if ((tagAssoc.objectUid).split("TAG:", 2).size() > 0) {
                                        def tagValue = AmTagValue.findById((tagAssoc.objectUid).split("TAG:", 2)[1]);
                                        if (tagValue != null) value = tagValue.value
                                    }
                                }
                                zipStream.write((amTagItem.displayName + ": " + value + "\r\n").getBytes());
                            } else if (amTagItem.tagItemSubtype == 'PICKLIST') {
                                def value = ""
                                def tagAssoc = AmTagAssociation.find("from AmTagAssociation where subjectUid=? and tagItemId=?", ["FOL:" + folderId, amTagItem.id])
                                if (tagAssoc != null) {
                                    def valueUId = tagAssoc.objectUid
                                    def bioData = BioData.findByUniqueId(valueUId)
                                    if (bioData != null) {
                                        def concept = ConceptCode.findById(bioData.id)
                                        if (concept != null) {
                                            value = concept.codeName
                                        }
                                    }
                                }
                                zipStream.write((amTagItem.displayName + ": " + value + "\r\n").getBytes());
                            } else if (amTagItem.tagItemSubtype == 'MULTIPICKLIST') {
                                def values = ""
                                def tagAssocs = AmTagAssociation.findAll("from AmTagAssociation where subjectUid=? and tagItemId=?", ["FOL:" + folderId, amTagItem.id])
                                for (tagAssoc in tagAssocs) {
                                    def valueUId = tagAssoc.objectUid
                                    def bioData = BioData.findByUniqueId(valueUId)
                                    if (bioData != null) {
                                        def concept = ConceptCode.findById(bioData.id)
                                        if (concept != null) {
                                            if (values != "") values += "; "
                                            values += concept.codeName
                                        }
                                    }
                                }
                                zipStream.write((amTagItem.displayName + ": " + values + "\r\n").getBytes());
                            }

                        } else if (amTagItem.tagItemType == 'BIO_ASSAY_PLATFORM') {
                            def values = ""
                            def tagAssocs = AmTagAssociation.findAll("from AmTagAssociation where subjectUid=? and objectType=?", ["FOL:" + folderId, amTagItem.tagItemType])
                            for (tagAssoc in tagAssocs) {
                                def tagValue = (tagAssoc.objectUid).split(":", 2)[1];
                                def bap = BioAssayPlatform.findByAccession(tagValue)
                                if (bap != null) {
                                    if (values != "") values += "; "
                                    values += bap.platformType + "/" + bap.platformTechnology + "/" + bap.vendor + "/" + bap.name
                                }
                            }
                            zipStream.write((amTagItem.displayName + ": " + values + "\r\n").getBytes());
                        } else {//bio_disease, bio_coumpound...
                            def values = ""
                            def tagAssocs = AmTagAssociation.findAll("from AmTagAssociation where subjectUid=? and objectType=?", ["FOL:" + folderId, amTagItem.tagItemType])
                            for (tagAssoc in tagAssocs) {
                                def key = SearchKeyword.findByUniqueId(tagAssoc.objectUid)
                                if (key != null) {
                                    if (values != "") values += "; "
                                    values += key.keyword
                                } else {
                                    def bioData = BioData.findByUniqueId(tagAssoc.objectUid)
                                    if (bioData != null) {
                                        def concept = ConceptCode.findById(bioData.id)
                                        if (concept != null) {
                                            if (values != "") values += "; "
                                            values += concept.codeName
                                        }
                                    }
                                }
                            }
                            zipStream.write((amTagItem.displayName + ": " + values + "\r\n").getBytes());
                        }
                    }
                    zipStream.write(("\r\n").getBytes())
                }
            }
            zipStream.closeEntry()
        } catch (Exception e) {
            log.error("Error writing ZIP", e)
        }
    }

    def exportStudyFiles = {
        def ids = []
        def folder = fmFolderService.getFolderByBioDataObject(Experiment.findByAccession(params.accession))

        def files = folder.fmFiles
        for (file in files) {
            if (file.activeInd) {
                ids.add(file.id)
            }
        }
        ids = ids.join(",")
        redirect(action: export, params: [id: ids])
    }

    def exportFile = {
        def id = params.id
        def filestorePath = grailsApplication.config.com.recomdata.FmFolderService.filestoreDirectory

        FmFile fmFile = FmFile.get(id)
        def fileLocation = filestorePath + "/" + fmFile.filestoreLocation + "/" + fmFile.filestoreName
        File file = new File(fileLocation)
        if (file.exists()) {
            String dirName = fmFolderService.getPath(fmFile.folder, true)

            //Construct a file name out of the display name + suffix, if needed
            def exportName = fmFile.displayName;
            if (!exportName.endsWith("." + fmFile.fileType)) {
                exportName += "." + fmFile.fileType
            }
            def mimeType = URLConnection.guessContentTypeFromName(file.getName())
            if (!params.open) {
                response.setHeader('Content-disposition', 'attachment; filename=' + exportName)
            }
            response.setHeader('Content-Type', mimeType)
            file.withInputStream({ is -> response.outputStream << is })
            response.outputStream.flush()
        } else {
            render(status: 500, text: "This file (" + fileLocation + ") was not found in the repository.")
        }
    }
}
