package org.transmartproject.search.browse

import grails.util.Holders

class FolderStudyMappingView implements Serializable {

    Long folderId
    String uniqueId
    String conceptPath
    Boolean root

    static mapping = {
        table schema: 'biomart_user', name: 'folder_study_mapping'
        id composite: ['folderId']

        conceptPath column: 'c_fullname'
        if (Holders.grailsApplication.config.dataSource.dialect.contains('Oracle')) {
            root type: 'yes_no'
        }
        version false
    }

    static constraints = {
        uniqueId       maxSize: 300
        conceptPath    maxSize: 700
    }
}
