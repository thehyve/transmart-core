package org.transmartproject.rest.serialization

import org.transmartproject.core.multidimquery.hypercube.ValueType
import org.transmartproject.rest.hypercubeProto.ObservationsProto

/**
 * Serialises a hypercube type and associated values to protobuf.
 */
class TypeProtobufSerializer {

    /**
     * Add a value compatible with this Type to a DimensionElementFieldColumn
     */
    static void addToColumn(ValueType type, ObservationsProto.DimensionElementFieldColumn.Builder builder, elem) {
        switch(type) {
            case ValueType.STRING:
                builder.addStringValue((String) elem)
                break
            case ValueType.INT:
                builder.addIntValue((Long) elem)
                break
            case ValueType.DOUBLE:
                builder.addDoubleValue((Double) elem)
                break
            case ValueType.TIMESTAMP:
                builder.addTimestampValue(((Date) elem).time)
                break
            case ValueType.MAP:
                // It is simpler to special-case serialising a map into protobuf messages in the calling code than to
                // extend this method to support maps
                throw new UnsupportedOperationException("not implemented for type MAP, use custom code")
            default:
                throw new RuntimeException("Unsupported type: ${type}. This type is not serializable")
        }
    }

    /**
     * Set a value compatible with this Type on a Value.Builder
     */
    static void setValue(ValueType type, ObservationsProto.Value.Builder builder, elem) {
        switch(type) {
            case ValueType.STRING:
                builder.stringValue = (String) elem
                break
            case ValueType.INT:
                builder.intValue = (Long) elem
                break
            case ValueType.DOUBLE:
                builder.doubleValue = (Double) elem
                break
            case ValueType.TIMESTAMP:
                builder.timestampValue = ((Date) elem).time
                break
            case ValueType.MAP:
                Map map = (Map) elem
                for(def entry : map) {
                    def keyBuilder = ObservationsProto.Value.newBuilder()
                    setValue(ValueType.forClass(entry.key.class), keyBuilder, entry.key)
                    def valueBuilder = ObservationsProto.Value.newBuilder()
                    setValue(ValueType.forClass(entry.value.class), valueBuilder, entry.value)
                    def objectValue = ObservationsProto.MapEntry.newBuilder()
                            .setKey(keyBuilder)
                            .setValue(valueBuilder)
                            .build()
                    builder.addObjectValue(objectValue)
                }
                break
            default:
                throw new RuntimeException("Unsupported type: ${type}. This type is not serializable")
        }
    }

    static ObservationsProto.Type mapToProtobufType(ValueType type) {
        switch(type) {
            case ValueType.STRING:
                return ObservationsProto.Type.STRING
            case ValueType.INT:
                return ObservationsProto.Type.INT
            case ValueType.DOUBLE:
                return ObservationsProto.Type.DOUBLE
            case ValueType.TIMESTAMP:
                return ObservationsProto.Type.TIMESTAMP
            case ValueType.MAP:
                return ObservationsProto.Type.OBJECT
            default:
                throw new RuntimeException("Unsupported type: ${type}. This type is not serializable")
        }
    }

}
