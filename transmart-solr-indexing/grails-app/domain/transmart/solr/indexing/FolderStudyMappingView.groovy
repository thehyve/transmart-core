package transmart.solr.indexing

import grails.util.Holders

class FolderStudyMappingView implements Serializable {

    Long folderId
    String id
    String conceptPath
    Boolean root

    static mapping = {
        table schema: 'biomart_user', name: 'folder_study_mapping'
        id column: 'unique_id', insert: false, update: false
        conceptPath column: 'c_fullname'
        //if (Holders.grailsApplication.config.dataSource.dialect.contains('Oracle')) {
        root type: 'yes_no'
        //}
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
