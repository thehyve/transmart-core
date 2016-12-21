package org.transmartproject.rest.serialization

import com.google.common.collect.PeekingIterator
import com.google.protobuf.Message
import grails.util.Pair
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.core.multidimquery.Property
import org.transmartproject.db.util.IndexedArraySet

import static org.transmartproject.rest.hypercubeProto.ObservationsProto.*

@Slf4j
@CompileStatic
class HypercubeProtobufSerializer {

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
            builder.addInlineDimensions(buildDimensionElement(dim, value[dim]))
        }
        builder
    }

    Value.Builder transferValue = Value.newBuilder()

    private Value.Builder buildValue(Object value) {
        def builder = transferValue.clear()
        builder.clear()
        switch (value) {
            case null:
                break
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

    DimensionElement buildDimensionElement(Dimension dim, Object value) {
        def builder = transferDimElem.clear()
        if (dim.elementsSerializable) {
            Value.Builder v = buildValue(dim.asSerializable(value))
            builder.intValue = v.intValue
            builder.doubleValue = v.doubleValue
            builder.timestampValue = v.timestampValue
            builder.stringValue = v.stringValue
        } else if(value != null) {
            for (prop in dim.elementFields.values()) {
                builder.addFields(buildValue(prop.get(value)).build())
            }
        }
        return builder.build()
    }

    protected DimensionElements buildDimensionElements(Dimension dim, List dimElements) {
        def builder = DimensionElements.newBuilder()
        builder.name = dim.name
        //builder.perSample = false //TODO: implement this
        for(prop in dim.elementFields.values()) {
            builder.addFields(buildElementFields(prop, dimElements))
        }
//        for(element in dimElements) {
//            builder.addFields(buildDimensionElement(dim, element))
//        }
        builder.build()
    }

    private DimensionElementFieldColumn.Builder transferFieldColumn = DimensionElementFieldColumn.newBuilder()

    protected DimensionElementFieldColumn buildElementFields(Property prop, List dimElements) {
        def builder = transferFieldColumn.clear()
        switch(prop.type) {
            case String:
                for(elem in dimElements) { builder.addStringValue((String) prop.get(elem)) }; break
            case Integer:
            case Long:
            case Short:
                for(elem in dimElements) { builder.addIntValue(((Number) prop.get(elem)).longValue()) }; break
            case Double:
            case Float:
            case Number:
                for(elem in dimElements) { builder.addDoubleValue(((Number) prop.get(elem)).doubleValue()) }; break
            case Date:
                for(elem in dimElements) { builder.addTimestampValue(((Date) prop.get(elem)).time) }; break
            default:
                throw new RuntimeException("Unknown type: ${prop.type}")
        }

        builder.build()
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

