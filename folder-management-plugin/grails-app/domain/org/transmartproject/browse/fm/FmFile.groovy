package org.transmartproject.browse.fm

import groovy.transform.ToString

@ToString(includes = ['id', 'displayName', 'originalName', 'fileSize', 'filestoreLocation', 'filestoreName', 'createDate'])
class FmFile {

    Long id
    String displayName
    String originalName
    Long fileVersion = 1l
    String fileType
    Long fileSize
    String filestoreLocation
    String filestoreName
    String linkUrl
    Boolean activeInd = Boolean.TRUE
    Date createDate = new Date()
    Date updateDate = new Date()
    String uniqueId
    String fileDescription

    static hasMany = [folders: FmFolder]
    //Should probably only have one, but Grails doesn't allow join table on one-many

    static belongsTo = FmFolder

    static transients = ['folder', 'uniqueId']

    static mapping = {
        table schema: 'fmapp'
        version false
        cache true
        sort "displayName"
        id column: 'file_id', generator: 'sequence', params: [sequence: 'seq_fm_id']
        folders joinTable: [name: 'fm_folder_file_association', key: 'file_id', column: 'folder_id']
        fileDescription column: 'DESCRIPTION'
    }

    static constraints = {
        displayName(maxSize: 1000)
        originalName(maxSize: 1000)
        fileType(nullable: true, maxSize: 100)
        fileSize(nullable: true)
        filestoreLocation(nullable: true, maxSize: 1000)
        filestoreName(nullable: true, maxSize: 1000)
        linkUrl(nullable: true, maxSize: 1000)
        fileDescription(nullable: true, maxSize:2000)
    }

    /**
     * Gets file's associated folder.
     * @return
     */
    FmFolder getFolder() {
        if (folders != null && !folders.isEmpty()) {
            return folders.iterator().next();
        }
        return null;
    }

    /**
     * Sets file's associated folder.
     * @param folder
     */
    def setFolder(FmFolder folder) {
        this.addToFolders(folder);
    }

    /**
     * Use transient property to support unique ID for folder.
     * @return folder's uniqueId
     */
    String getUniqueId() {
        if (uniqueId == null) {
            FmData data = FmData.get(id);
            if (data != null) {
                uniqueId = data.uniqueId
                return data.uniqueId;
            }
            return null;
        }
        return uniqueId;
    }

    /**
     * Find file by its uniqueId
     * @param uniqueId
     * @return file with matching uniqueId or null, if match not found.
     */
    static FmFile findByUniqueId(String uniqueId) {
        FmFile file;
        FmData data = FmData.findByUniqueId(uniqueId);
        if (data != null) {
            file = FmFile.get(data.id);
        }
        return file;
    }

}
