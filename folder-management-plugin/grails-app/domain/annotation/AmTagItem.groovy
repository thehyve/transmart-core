package annotation


class AmTagItem implements Comparable<AmTagItem> {

    Long id
    String displayName
    Integer displayOrder
//	String tagItemUid
    Integer maxValues
    String guiHandler
    String codeTypeName
    String tagItemType
    String tagItemSubtype
    String tagItemAttr
    Boolean editable = Boolean.TRUE
    Boolean required = Boolean.TRUE
    Boolean activeInd = Boolean.TRUE
    Boolean viewInGrid = Boolean.TRUE
    Boolean viewInChildGrid = Boolean.TRUE
    static belongsTo = [amTagTemplate: AmTagTemplate]

//	AmTagAssociation amTagAssociation

    static mapping = {
        table 'am_tag_item'
        version false
        cache true
        sort "displayOrder"
        amTagTemplate joinTable: [name: 'am_tag_template', key: 'tag_template_id', column: 'tag_item_id'], lazy: false
        id column: 'tag_item_id', generator: 'sequence', params: [sequence: 'AMAPP.SEQ_AMAPP_DATA_ID']
        amTagTemplate column: 'tag_template_id'
//		amTagAssociation joinTable: [name: 'am_tag_association',  key:'tag_item_id', column: 'tag_item_id'], lazy: false

    }

    @Override
    public int compareTo(AmTagItem itemIn) {

        if (itemIn.displayOrder != null && displayOrder != null) {
            return displayOrder?.compareTo(itemIn.displayOrder);
        } else {
            return displayName?.compareTo(itemIn.displayName);
        }
        return 0;
    }

    static constraints = {
        tagItemType(maxSize: 200)
        tagItemAttr(maxSize: 200)
        displayName(maxSize: 200)
        codeTypeName(maxSize: 200)
        guiHandler(maxSize: 200)
        tagItemSubtype(nullable: true)
//		tagItemUid(maxSize:300)
    }

    /**
     * override display
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("ID: ").append(this.id).append(", Display Name: ").append(this.displayName);
        sb.append(", Display Order: ").append(this.displayOrder);
        return sb.toString();
    }

}
