package org.transmartproject.db.util

class ClassUtils {

    /**
     * Finds common java class for the two classes.
     * @param class1
     * @param class2
     * @return common class. it returns null if both classes are null
     */
    static Class getCommonClass(Class class1, Class class2) {
        if (class1 && class2) {
            Class valuesCommonType = class1
            while (!valuesCommonType.isAssignableFrom(class2)) {
                valuesCommonType = valuesCommonType.superclass
            }
            valuesCommonType
        } else {
            class1 ?: class2
        }
    }

}
