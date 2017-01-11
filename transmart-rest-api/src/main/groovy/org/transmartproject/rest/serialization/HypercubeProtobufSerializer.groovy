package org.transmartproject.rest.serialization

import com.google.common.collect.PeekingIterator
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.core.multidimquery.Property
import org.transmartproject.rest.hypercubeProto.ObservationsProto.Type as ProtoType

import javax.annotation.Nonnull

import static org.transmartproject.rest.hypercubeProto.ObservationsProto.*

@Slf4j
@CompileStatic
class HypercubeProtobufSerializer extends HypercubeSerializer {

    protected Hypercube cube
    protected Dimension packedDimension
    protected boolean packingEnabled
    protected OutputStream out

    private PeekingIterator<HypercubeValue> iterator
    private List<Dimension> inlineDims
    private List<Dimension> indexedDims


    static protected enum Type {
        STRING {
            ProtoType getProtobufType() {ProtoType.STRING}
            void addToColumn(DimensionElementFieldColumn.Builder builder, elem) {
                builder.addStringValue((String) elem)
            }
            void setValue(Value.Builder builder, elem) {
                builder.stringValue = (String) elem
            }
        },

        INT {
            ProtoType getProtobufType() {ProtoType.INT}
            void addToColumn(DimensionElementFieldColumn.Builder builder, elem) {
                builder.addIntValue((Long) elem)
            }
            void setValue(Value.Builder builder, elem) {
                builder.intValue = (Long) elem
            }
        },

        DOUBLE {
            ProtoType getProtobufType() {ProtoType.DOUBLE}
            void addToColumn(DimensionElementFieldColumn.Builder builder, elem) {
                builder.addDoubleValue((Double) elem)
            }
            void setValue(Value.Builder builder, elem) {
                builder.doubleValue = (Double) elem
            }
        },

        TIMESTAMP {
            ProtoType getProtobufType() {ProtoType.TIMESTAMP}
            void addToColumn(DimensionElementFieldColumn.Builder builder, elem) {
                builder.addTimestampValue(((Date) elem).time)
            }
            void setValue(Value.Builder builder, elem) {
                builder.timestampValue = ((Date) elem).time
            }
        }

        /**
         * @return the protobuf type (ObservationsProto.Type) corresponding to this enum type. This enum can be
         * considered a wrapper around ObservationsProto.Type that adds extra methods.
         */
        // Groovy didn't want to compile a 'type' field, so I use a method
        abstract ProtoType getProtobufType()

        /**
         * Add a value compatible with this Type to a DimensionElementFieldColumn
         */
        abstract void addToColumn(DimensionElementFieldColumn.Builder builder, elem)

        /**
         * Set a value compatible with this Type on a Value.Builder
         */
        abstract void setValue(Value.Builder builder, elem)


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
                default:
                    throw new RuntimeException("Unsupported type: $cls. This type is not serializable")
            }
        }
    }


    protected List<DimensionDeclaration> getDimensionsDefs() {
        cube.dimensions.collect { Dimension dim ->
            DimensionDeclaration.Builder builder = DimensionDeclaration.newBuilder()

            builder.name = dim.name
            if (!dim.density.isDense) {
                // Sparse dimensions are inlined, dense dimensions are referred to by indexes
                // (referring to objects in the footer message).
                builder.inline = true
            }
            if (dim == packedDimension) {
                builder.packed = true
            }
            builder.type = dim.elementsSerializable ? Type.get(dim.elementType).protobufType : ProtoType.OBJECT

            if(!dim.elementsSerializable) {
                dim.elementFields.values().each { field ->
                    builder.addFields FieldDefinition.newBuilder().with {
                        name = field.name
                        type = Type.get(field.type).protobufType
                        assert type != ProtoType.OBJECT
                        build()
                    }
                }
            }

            builder.build()
        }
    }

    protected Header buildHeader() {
        Header.newBuilder().with {
            addAllDimensionDeclarations(dimensionsDefs)
            if(!iterator.hasNext()) last = true
            build()
        }
    }

    protected Cell.Builder createCell(HypercubeValue value) {
        def builder = Cell.newBuilder()
        if (value.value != null) {
            if (value.value instanceof Number) {
                builder.numericValue = ((Number) value.value).doubleValue()
            } else {
                builder.stringValue = value.value
            }
        }
        for (int i=0; i<indexedDims.size(); i++) {
            Integer idx = value.getDimElementIndex(indexedDims[i])
            if(idx != null) {
                builder.addDimensionIndexes(value.getDimElementIndex(indexedDims[i]))
            } else {
                //FIXME: add support for this in the protobuf format
                throw new UnsupportedOperationException("No dimension element for value $value, dimension ${indexedDims[i]}")
            }
        }
        for (int i=0; i<inlineDims.size(); i++) {
            Dimension dim = inlineDims[i]
            def dimElement = value[dim]
            if(dimElement != null) {
                builder.addInlineDimensions(buildDimensionElement(dim, dimElement))
            } else {
                // these values are 1-based!
                builder.addAbsentInlineDimensions(i+1)
            }
        }
        if(!iterator.hasNext()) builder.last = true
        builder
    }

    Value.Builder transferValue = Value.newBuilder()

    private Value.Builder buildValue(@Nonnull value) {
        def builder = transferValue.clear()
        builder.clear()
        Type.get(value.class).setValue(builder, value)
        builder
    }

    private DimensionElement.Builder transferDimElem = DimensionElement.newBuilder()

    DimensionElement buildDimensionElement(Dimension dim, @Nonnull Object value) {
        def builder = transferDimElem.clear()
        if (dim.elementsSerializable) {
            Value.Builder v = buildValue(dim.asSerializable(value))
            if(v == null) return null
            builder.intValue = v.intValue
            builder.doubleValue = v.doubleValue
            builder.timestampValue = v.timestampValue
            builder.stringValue = v.stringValue
        } else {
            List<Property> elementProperties = dim.elementFields.values().asList()
            for (int i=0; i<elementProperties.size(); i++) {
                def fieldVal = elementProperties[i].get(value)
                if(fieldVal == null) {
                    // 1-based!
                    builder.addAbsentFieldIndices(i+1)
                } else {
                    builder.addFields(buildValue(fieldVal).build())
                }
            }
        }
        return builder.build()
    }

    protected DimensionElements buildDimensionElements(Dimension dim, List dimElements, boolean setName=true) {
        def builder = DimensionElements.newBuilder()
        if(setName) builder.name = dim.name
        //builder.perSample = false //TODO: implement this

        def properties = dim.elementFields.values().asList()
        for(int i=0; i<properties.size(); i++) {
            def fieldColumn = buildElementFields(properties[i], dimElements)
            if(fieldColumn == null) {
                builder.addAbsentFieldColumnIndices(i+1)
            } else {
                builder.addFields(fieldColumn)
            }
        }
//        for(element in dimElements) {
//            builder.addFields(buildDimensionElement(dim, element))
//        }
        builder.build()
    }


    private DimensionElementFieldColumn.Builder transferFieldColumn = DimensionElementFieldColumn.newBuilder()

    protected DimensionElementFieldColumn buildElementFields(Property prop, List dimElements) {
        DimensionElementFieldColumn.Builder builder = transferFieldColumn.clear()

        Type type = Type.get(prop.type)

        long absentCount = 0
        for(int i=0; i<dimElements.size(); i++) {
            def elem = prop.get(dimElements[i])
            if(elem == null) {
                absentCount++
                builder.addAbsentValueIndices(i+1)
            } else {
                type.addToColumn(builder, elem)
            }
        }

        if (absentCount == dimElements.size()) {
            null
        } else {
            builder.build()
        }
    }

    protected Footer buildFooter() {
        def builder = Footer.newBuilder()
        for(dim in cube.dimensions.findAll { it.density.isDense }) {
            builder.addDimension(buildDimensionElements(dim, cube.dimensionElements(dim)))
        }
        builder.build()
    }

    void write(Map args, Hypercube cube, OutputStream out) {
        this.cube = cube
        this.out = out
        this.packedDimension = (Dimension) args.packedDimension
        this.packingEnabled = packedDimension != null

        this.iterator = cube.iterator()
        this.inlineDims = cube.dimensions.findAll { it != packedDimension && !it.density.isDense }
        this.indexedDims = cube.dimensions.findAll { it != packedDimension && it.density.isDense }


        buildHeader().writeDelimitedTo(out)

        while(iterator.hasNext()) {
            def message = createCell(iterator.next())
            message.build().writeDelimitedTo(out)
        }

        buildFooter().writeDelimitedTo(out)
    }
}

