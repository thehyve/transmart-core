/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest.serialization

import org.transmartproject.rest.hypercubeProto.ObservationsProto

import static org.transmartproject.rest.hypercubeProto.ObservationsProto.*

enum Type {
//    DOUBLE('Double'),
//    INTEGER('Integer'),
//    STRING('String'),
//    DATE('Date'),
//    OBJECT('Object'),
//    ID('Id')

    STRING {
        String getJsonType() {"String"}
        ObservationsProto.Type getProtobufType() {ObservationsProto.Type.STRING}
        void addToColumn(DimensionElementFieldColumn.Builder builder, elem) {
            builder.addStringValue((String) elem)
        }
        void setValue(Value.Builder builder, elem) {
            builder.stringValue = (String) elem
        }
    },

    INT {
        String getJsonType() {"Int"}
        ObservationsProto.Type getProtobufType() {ObservationsProto.Type.INT}
        void addToColumn(DimensionElementFieldColumn.Builder builder, elem) {
            builder.addIntValue((Long) elem)
        }
        void setValue(Value.Builder builder, elem) {
            builder.intValue = (Long) elem
        }
    },

    DOUBLE {
        String getJsonType() {"Double"}
        ObservationsProto.Type getProtobufType() {ObservationsProto.Type.DOUBLE}
        void addToColumn(DimensionElementFieldColumn.Builder builder, elem) {
            builder.addDoubleValue((Double) elem)
        }
        void setValue(Value.Builder builder, elem) {
            builder.doubleValue = (Double) elem
        }
    },

    TIMESTAMP {
        String getJsonType() {"Timestamp"}
        ObservationsProto.Type getProtobufType() {ObservationsProto.Type.TIMESTAMP}
        void addToColumn(DimensionElementFieldColumn.Builder builder, elem) {
            builder.addTimestampValue(((Date) elem).time)
        }
        void setValue(Value.Builder builder, elem) {
            builder.timestampValue = ((Date) elem).time
        }
    },

    MAP {
        String getJsonType() {"Object"}
        ObservationsProto.Type getProtobufType() {ObservationsProto.Type.OBJECT}
        void addToColumn(DimensionElementFieldColumn.Builder builder, elem) {
            // It is simpler to special-case serialising a map into protobuf messages in the calling code than to
            // extend this method to support maps
            throw new UnsupportedOperationException("not implemented for type MAP, use custom code")
        }
        void setValue(Value.Builder builder, elem) {
            Map map = (Map) elem
            for(def entry : map) {
                def key = Value.newBuilder()
                get(entry.key.class).setValue(key, entry.key)
                def value = Value.newBuilder()
                get(entry.value.class).setValue(value, entry.value)

                builder.addObjectValue(MapEntry.newBuilder().setKey(key).setValue(value).build())
            }
        }
    }

    /**
     * @return the protobuf type (ObservationsProto.Type) corresponding to this enum type. This enum can be
     * considered a wrapper around ObservationsProto.Type that adds extra methods.
     */
    // Groovy didn't want to compile a 'type' field, so I use a method
    abstract ObservationsProto.Type getProtobufType()

    /**
     * Add a value compatible with this Type to a DimensionElementFieldColumn
     */
    abstract void addToColumn(DimensionElementFieldColumn.Builder builder, elem)

    /**
     * Set a value compatible with this Type on a Value.Builder
     */
    abstract void setValue(Value.Builder builder, elem)


    abstract String getJsonType()


    static protected Type get(Class cls) {
        switch (cls) {
            case String:
                return STRING
            case Integer:
            case Long:
            case Short:
                return INT
            case Double:
            case Float:
            case Number:
                return DOUBLE
            case Date:
                return TIMESTAMP
            case Map:
                return MAP
            default:
                throw new RuntimeException("Unsupported type: $cls. This type is not serializable")
        }
    }
}
