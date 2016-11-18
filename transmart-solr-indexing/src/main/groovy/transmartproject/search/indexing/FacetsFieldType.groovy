package org.transmartproject.search.indexing

enum FacetsFieldType {

    DATE('d', Date),
    STRING('s', String) /* NOT ANALYZED */,
    STRING_LOWERCASE('l', String) /* internal type */,
    TEXT('t', String)   /* ANALYZED */,
    INTEGER('i', Long),
    FLOAT('f', Double);

    String suffix
    Class<?> requiredClass

    FacetsFieldType(String suffix, Class<?> requiredClass) {
        this.suffix = suffix
        this.requiredClass = requiredClass
    }

    boolean isValueCompatible(Object obj) {
        requiredClass.isAssignableFrom(obj.getClass())
    }
}
