package org.transmartproject.db.marshallers

class EnumMarshaller {
    static targetType = Enum

    def convert(Enum enumeration) {
        enumeration.name()
    }
}
