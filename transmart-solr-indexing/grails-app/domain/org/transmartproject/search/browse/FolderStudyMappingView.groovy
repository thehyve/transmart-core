package org.transmartproject.search.browse

class FolderStudyMappingView implements Serializable {

    Long folderId
    String uniqueId
    String conceptPath
    Boolean root

    static mapping = {
        table schema: 'biomart_user', name: 'folder_study_mapping'
        id composite: ['folderId']

        conceptPath column: 'c_fullname'
        version false
    }

    static constraints = {
        uniqueId       maxSize: 300
        conceptPath    maxSize: 700
    }
}
