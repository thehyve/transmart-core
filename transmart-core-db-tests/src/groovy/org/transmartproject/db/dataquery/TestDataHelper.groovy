package org.transmartproject.db.dataquery
/**
 * Helper class for dealing with test data.
 */
class TestDataHelper {

    /**
     * Fills the object with dummy values for all the fields that are mandatory (nullable = false) and have no value
     * @param clazz
     * @param obj
     */
    static <T> void completeObject(Class<T> clazz, T obj) {
        List<MetaProperty> fields = getMandatoryProps(clazz).findAll { it.getProperty(obj) == null } //all without value
        for (MetaProperty f: fields) {
            f.setProperty(obj, getDummyObject(f.type)) //set a dummy value
        }
    }

    static Object getDummyObject(Class type) {

        switch (type) {
            case String:
                return ''
            case Character:
                return ''
            case Integer:
                return 0
            case Date:
                return new Date()
            default:
                throw new UnsupportedOperationException("Not supported: $type.name. Care to add it?")
        }
    }

    private static List<MetaProperty> getMandatoryProps(Class clazz) {
        def mandatory = clazz.constraints?.findAll { it.value.nullable == false } //get all not nullable properties
        clazz.metaClass.properties.findAll { mandatory.containsKey(it.name) }
    }

}
