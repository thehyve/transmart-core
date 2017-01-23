package annotation

class AmTagTemplate {

    Long id
    String tagTemplateName
    String tagTemplateType
    String tagTemplateSubtype
    Boolean activeInd = Boolean.TRUE

    SortedSet amTagItems

    static hasMany = [amTagItems: AmTagItem]//, fmFolders: FmFolder]

    static mapping = {
        table 'am_tag_template'
        version false
        cache true
        sort "tagTemplateName"
        id column: 'tag_template_id', generator: 'sequence', params: [sequence: 'AMAPP.SEQ_AMAPP_DATA_ID']
//		amTagItems joinTable: [name: 'tag_template_item_def',  key:'tag_template_id', column: 'tag_item_id'], 
        amTagItems lazy: false
//		fmFolders joinTable: [name: 'am_tag_template_association', key:'tag_template_id', column: 'object_uid'], lazy: false
    }


    static constraints = {
        tagTemplateName(maxSize: 200)
        tagTemplateType(maxSize: 50)
        tagTemplateSubtype(maxSize: 50)
    }

    /**
     * override display
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("ID: ").append(this.id).append(", Template Name: ").append(this.tagTemplateName);
        sb.append(", Template Type: ").append(this.tagTemplateType);
        return sb.toString();
    }
}
