package annotation

class AmTagAssociation implements Serializable {

    String objectType
    String subjectUid
    String objectUid
    Long tagItemId


    static mapping = {
        table 'am_tag_association'
        version false
        cache true
        sort "tagTemplateName"
        id composite: ["objectUid", "subjectUid"]
//		amTagItem column: 'tag_item_id', insert: "false", update: "false"

    }


    static constraints =
            {
            }

    static AmTagAssociation get(String objectUid, Long tagItemId, String subjectUid) {
        find 'from AmTagAssociation where objectUid=:objectUid and tagItemId=:tagItemId and subjectUid=:subjectUid',
                [objectUid: objectUid, tagItemId: tagItemId.toString(), subjectUid: subjectUid]
    }

    static boolean remove(String objectUid, Long tagItemId, String subjectUid, boolean flush = false) {
        AmTagAssociation instance = AmTagAssociation.findByObjectUidAndTagItemIdAndSubjectUid(objectUid, tagItemId.toString(), subjectUid)
        instance ? instance.delete(flush: flush) : false
    }

    /**
     * override display
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("objectType: ").append(this.objectType).append(", subjectUid: ").append(this.subjectUid);
        sb.append(", objectUid: ").append(this.objectUid).append(", tagItemId: ").append(this.tagItemId.toString());
        return sb.toString();
    }


}


	