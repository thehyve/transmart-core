package org.transmartproject.solr

class FolderStudyMappingView implements Serializable {

    Long folderId
    String id
    String conceptPath
    Boolean root

    static mapping = {
        table schema: 'biomart_user', name: 'folder_study_mapping'
        id column: 'unique_id', insert: false, update: false
        conceptPath column: 'c_fullname'
        version false
    }

    static constraints = {
        id             maxSize: 300
        conceptPath    maxSize: 700
    }

    def getUniqueId() {
        id
    }
}
