package annotation


class AmTagTemplate2 {

    Long id
    String tagTemplateName
    String tagTemplateType
    String guiHandler

    static mapping = {
        table 'am_tag_template'
        version false
        cache true
        sort "tagTemplateName"
        columns { id column: 'tag_template_id' }
    }


}
