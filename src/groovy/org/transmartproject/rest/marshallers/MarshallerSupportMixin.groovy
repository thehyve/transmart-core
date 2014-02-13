package org.transmartproject.rest.marshallers

class MarshallerSupportMixin {

    Map<String, Object> getPropertySubsetForSuperType(Object o, Class superType) {
        if (!superType.isAssignableFrom(o.getClass())) {
            throw new IllegalArgumentException("Object '$o' is not of type " +
                    "$superType")
        }

        o.properties.subMap(superType.metaClass.properties*.name)
    }

}
