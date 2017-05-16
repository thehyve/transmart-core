package annotation

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class AmTagDisplayValue implements Serializable {

    AmTagItem amTagItem
    String subjectUid
    String displayValue
    String objectType
    String objectUid
    Long objectId
    String uniqueId
    String codeName

    static transients = ['uniqueId', 'codeName']

    /**
     * Use transient property to support unique ID for tagValue.
     * @return tagValue's uniqueId
     */
    String getUniqueId() {
        if (uniqueId == null) {
            uniqueId = objectUid
        }

        return uniqueId;
    }

    String getCodeName() {
        if (codeName == null) {
            codeName = displayValue
        }

        return codeName;
    }


    static mapping = {
        table schema: 'amapp', name: 'am_tag_display_vw'
        version false
        cache true
        sort "value"
        id composite: ["subjectUid", "objectUid", "amTagItem"]
        amTagItem column: 'tag_item_id'

    }

    static constraints = {
    }

    static AmTagDisplayValue get(String subjectUid, long objectId) {
        find 'from AmTagDisplayValue where subjectUid=:subjectUid and objectId=:objectId',
                [subjectUid: subjectUid, objectId: objectId]
    }

    static boolean remove(String objectUid, long objectId, boolean flush = false) {
        //	AmTagDisplayValue instance = FmFolderAssociation.findByObjectUidAndFmFolder(objectUid, fmFolder)
        //	instance ? instance.delete(flush: flush) : false

        false
    }

    static Collection<Object> findAllDisplayValue(String subjectUid, long amTagItemId) {
        findAll 'from AmTagDisplayValue where subjectUid=:subjectUid and amTagItem.id=:amTagItemId',
                [subjectUid: subjectUid, amTagItemId: amTagItemId]


    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Subject UID: ").append(subjectUid).append(", ");
        sb.append("Object UID: ").append(objectUid).append(", ");
        sb.append("Display Value: ").append(displayValue);
        return sb.toString();
    }

}
