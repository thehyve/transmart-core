package annotation

class AmTagTemplateAssociation {

    Long id
    Long tagTemplateId
    String objectUid

    static mapping = {
        table 'am_tag_template_association'
        version false
        cache true
        id generator: 'sequence', params: [sequence: 'AMAPP.SEQ_AMAPP_DATA_ID']
    }

    static constraints = {
        objectUid(maxSize: 200)
    }

}
