package annotation


class AmTagValue {

    Long id
    String value
    String uniqueId

    static transients = ['uniqueId']

    /**
     * Use transient property to support unique ID for tagValue.
     * @return tagValue's uniqueId
     */
    String getUniqueId() {
        if (uniqueId == null) {
            if (id) {
                AmData data = AmData.get(id);
                if (data != null) {
                    uniqueId = data.uniqueId
                    return data.uniqueId;
                }
                return null;
            } else {
                return null;
            }
        }
        return uniqueId;
    }

    /**
     * Find tagValue by its uniqueId
     * @param uniqueId
     * @return tagValue with matching uniqueId or null, if match not found.
     */
    static AmTagValue findByUniqueId(String uniqueId) {
        AmTagValue tagValue;
        AmData data = AmData.findByUniqueId(uniqueId);
        if (data != null) {
            tagValue = AmTagValue.get(data.id);
        }
        return tagValue;
    }


    static mapping = {
        table 'am_tag_value'
        version false
        cache true
        sort "value"
        id column: 'tag_value_id', generator: 'sequence', params: [sequence: 'AMAPP.SEQ_AMAPP_DATA_ID']
//		amTagItem joinTable: [name: 'am_tag_template',  key:'tag_item_id', column: 'tag_value_id'], lazy: false
//		amTagItem column: 'tag_item_id'

    }

    static constraints = {
        value(maxSize: 2000)
    }

}
