package org.transmartproject.db.dataquery

import java.lang.reflect.Field

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
        List<Field> fields = getMandatoryFields(clazz).findAll( {it.get(obj) == null} ) //all without value
        for (Field f: fields) {
            f.set(obj, getDummyObject(f.type)) //set a dummy value
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

    private static List<Field> getMandatoryFields(Class clazz) {
        def list = clazz.constraints?.findAll({ it.value.nullable == false }) //get all not nullable properties
        list.collect( {it.key} ).collect( { getField(clazz, it)}) //get the Field for them
    }

    private static Field getField(Class clazz, String name) {
        Field f = null;
        try {
            f = clazz.getDeclaredField(name)
        } catch (Exception ex) {
            //ignored
        }

        if (f != null) {
            f.setAccessible(true) //useful later on
            return f //found it
        } else if (clazz != Object) {
            return getField(clazz.superclass, name) //recurse into superclass, until we get the top
        } else {
            return null //no field
        }
    }

}
