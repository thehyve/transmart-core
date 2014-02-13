package org.transmartproject.rest.marshallers

class MarshallerSupportMixin {

    Map<String, Object> getPropertySubsetForSuperType(Object o,
                                                      Class superType,
                                                      Set<String> excludes = [] as Set) {
        if (!superType.isAssignableFrom(o.getClass())) {
            throw new IllegalArgumentException("Object '$o' is not of type " +
                    "$superType")
        }

        superType.metaClass.properties.findAll {
            !(it.name in excludes)
        }.collectEntries { MetaBeanProperty prop ->
            [prop.name, prop.getProperty(o)]
        }
    }

}
