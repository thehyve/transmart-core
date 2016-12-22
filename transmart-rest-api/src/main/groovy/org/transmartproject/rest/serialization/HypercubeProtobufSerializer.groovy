package org.transmartproject.rest.serialization

import com.google.common.collect.PeekingIterator
import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor
import groovy.util.logging.Slf4j
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.core.multidimquery.Property

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
            builder.type = dim.elementsSerializable ? type(dim.elementType) : Type.OBJECT

            if(!dim.elementsSerializable) {
                dim.elementFields.values().each { field ->
                    builder.addFields FieldDefinition.newBuilder().with {
                        name = field.name
                        type = type(field.type)
                        assert type != Type.OBJECT
                        build()
                    }
                }
            }

            builder.build()
        }
    }

    static protected Type type(Class cls) {
        switch (cls) {
            case String:
                return Type.STRING
            case Integer:
            case Long:
            case Short:
                return Type.INT
            case Double:
            case Float:
            case Number:
                return Type.DOUBLE
            case Date:
                return Type.TIMESTAMP
            default:
                return Type.OBJECT
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
        builder
    }

    Value.Builder transferValue = Value.newBuilder()

    private Value.Builder buildValue(@Nonnull value) {
        def builder = transferValue.clear()
        builder.clear()
        switch (value) {
            case null:
                return null
            case String:
                builder.stringValue = value; break
            case Date:
                builder.timestampValue = ((Date) value).time; break
            case Integer:
            case Long:
                builder.intValue = ((Number) value).longValue(); break
            case Number:
                builder.doubleValue = ((Number) value).doubleValue(); break
            default:
                throw new RuntimeException("Type not supported: $value of type ${value.class}")
        }
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


    @TupleConstructor
    static abstract class ColumnBuilder {
        Property prop
        DimensionElementFieldColumn.Builder builder

        abstract def value(element)
        abstract void addValue(item)
    }

    /* this method is part of the inner loop of the serializer, so preferably we shouldn't be using dynamic code
    or closures here. This does lead to slightly ugly code to get the right behavior for different types.
    Unfortunately Groovy doesn't do closures like java and also creates Reference's for final variables. In fact
    Groovy will always try to resolve identifiers used within the anonymous class methods in the scope of this
    function. The only way to prevent that and access the fields seems to be to explicitly qualify with 'this'.

    While the  JVM should be able to stack-allocate and then optimize away the ColumnBuilder and the extra Reference's,
    it doesn't hurt to prevent them, and doing so would not clean this code up significantly.
    */
    private static ColumnBuilder getColumnBuilder(Property prop, DimensionElementFieldColumn.Builder builder) {
        switch(prop.type) {
            case String:
                return new ColumnBuilder(prop, builder) {
                    // NB: `this` qualification is necessary, otherwise Groovy will allocate extra unneeded Reference's.
                    def value(elem) { this.prop.get(elem) }
                    void addValue(it) { this.builder.addStringValue((String) it) }
                }
            case Integer:
            case Long:
            case Short:
                return new ColumnBuilder(prop, builder) {
                    def value(elem) { ((Number) this.prop.get(elem))?.longValue() }
                    void addValue(it) { this.builder.addIntValue((Long) it) }
                }
            case Double:
            case Float:
            case Number:
                return new ColumnBuilder(prop, builder) {
                    def value(elem) { ((Number) this.prop.get(elem))?.doubleValue() }
                    void addValue(it) { this.builder.addDoubleValue((Double) it) }
                }
            case Date:
                return new ColumnBuilder(prop, builder) {
                    def value(elem) { ((Date) this.prop.get(elem))?.time }
                    void addValue(it) { this.builder.addTimestampValue((Long) it) }
                }
            default:
                throw new RuntimeException("Unknown type: ${prop.type}")
        }
    }

    private DimensionElementFieldColumn.Builder transferFieldColumn = DimensionElementFieldColumn.newBuilder()

    protected DimensionElementFieldColumn buildElementFields(Property prop, List dimElements) {
        DimensionElementFieldColumn.Builder builder = transferFieldColumn.clear()

        ColumnBuilder b = getColumnBuilder(prop, builder)

        long absentCount = 0
        for(int i=0; i<dimElements.size(); i++) {
            def elem = b.value(dimElements[i])
            if(elem == null) {
                absentCount++
                builder.addAbsentValueIndices(i+1)
            } else {
                b.addValue(elem)
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

