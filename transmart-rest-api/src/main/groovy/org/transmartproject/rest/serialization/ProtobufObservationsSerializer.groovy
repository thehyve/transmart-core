package org.transmartproject.rest.serialization

import com.google.protobuf.Empty
import com.google.protobuf.Message
import groovy.util.logging.Slf4j
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.multidimquery.EndTimeDimension
import org.transmartproject.db.multidimquery.LocationDimension
import org.transmartproject.db.multidimquery.ModifierDimension
import org.transmartproject.db.multidimquery.ProjectionDimension
import org.transmartproject.db.multidimquery.ProviderDimension
import org.transmartproject.db.multidimquery.StartTimeDimension
import org.transmartproject.db.multidimquery.StudyDimension
import org.transmartproject.db.multidimquery.query.DimensionMetadata

import static org.transmartproject.rest.hypercubeProto.ObservationsProto.*

@Slf4j
public class ProtobufObservationsSerializer extends AbstractObservationsSerializer {

    static Type getFieldType(Class type) {
        if (Float.isAssignableFrom(type)) {
            return Type.DOUBLE
        } else if (Double.isAssignableFrom(type)) {
            return Type.DOUBLE
        } else if (Number.isAssignableFrom(type)) {
            return Type.INT
        } else if (Date.isAssignableFrom(type)) {
            return Type.TIMESTAMP
        } else if (String.isAssignableFrom(type)) {
            return Type.STRING
        } else {
            // refer to objects by their identifier
            return Type.INT
        }
    }

    protected Dimension packedDimension
    protected boolean packingEnabled

    protected Map<Dimension, DimensionDeclaration> dimensionDeclarations = [:]

    ProtobufObservationsSerializer(Hypercube cube, Dimension packedDimension) {
        super(cube)
        this.packedDimension = packedDimension
        this.packingEnabled = packedDimension != null
    }

    protected boolean first = true

    protected void begin(OutputStream out) {
        first = true
    }

    protected void writeMessage(OutputStream out, Message message) {
        message.writeDelimitedTo(out)
        if (first) {
            first = false
        }
    }

    @Override
    protected void end(OutputStream out) {
        out.flush()
    }

    @Override
    void writeEmptyMessage(OutputStream out) {
        Empty empty = Empty.newBuilder().build()
        empty.writeDelimitedTo(out)
    }

    protected getDimensionsDefs() {
        def declarations = cube.dimensions.collect { dim ->
            def builder = DimensionDeclaration.newBuilder()
            String dimensionName
            if (dim instanceof ModifierDimension) {
                dimensionName = dim.name
            } else {
                dimensionName = dim.toString()
            }
            builder.setName(dimensionName)
            if (dim.density == Dimension.Density.SPARSE) {
                // Sparse dimensions are inlined, dense dimensions are referred to by indexes
                // (referring to objects in the footer message).
                builder.setInline(true)
            }
            if (dim == packedDimension) {
                builder.setPacked(true)
            }
            def publicFacingFields = SerializableProperties.SERIALIZABLES.get(dimensionName)
            switch(dim.class) {
                case ModifierDimension:
                    def modifierDim = (ModifierDimension)dim
                    switch (modifierDim.valueType) {
                        case ObservationFact.TYPE_NUMBER:
                            builder.type = Type.DOUBLE
                            break
                        case ObservationFact.TYPE_TEXT:
                            builder.type = Type.STRING
                            break
                        default:
                            throw new Exception("Unsupported value type for dimension ${dimensionName}: ${modifierDim.valueType}.")
                    }
                    break
                case StartTimeDimension:
                case EndTimeDimension:
                    builder.type = Type.TIMESTAMP
                    break
                case ProviderDimension:
                case LocationDimension:
                case ProjectionDimension:
                    builder.type = Type.STRING
                    break
                default:
                    builder.type = Type.OBJECT
                    def metadata = DimensionMetadata.forDimension(dim.class)
                    metadata.fields.each { field ->
                        if (field.fieldName in publicFacingFields) {
                            Class valueType = metadata.fieldTypes[field.fieldName]
                            def fieldDef = FieldDefinition.newBuilder()
                                    .setName(field.fieldName)
                                    .setType(getFieldType(valueType))
                                    .build()
                            builder.addFields(fieldDef)
                        }
                    }
                    break
            }
            def dimensionDeclaration = builder.build()
            dimensionDeclarations[dim] = dimensionDeclaration
            dimensionDeclaration
        }
        declarations
    }

    protected Header buildHeader() {
        Header.newBuilder().addAllDimensionDeclarations(dimensionsDefs).build()
    }

    protected void writeHeader(OutputStream out) {
        writeMessage(out, buildHeader())
    }

    protected List<HypercubeValue> currentValues = []

    /**
     * Checks if the value can be appended to <code>currentValues</code> to form
     * a combined packed observation message.
     */
    protected boolean canBeAppended(HypercubeValue value) {
        assert packingEnabled && packedDimension != null
        HypercubeValue sampleValue = currentValues[0]
        if (value.value == null) {
            throw new Exception("Null values not supported.")
        }
        boolean dimensionDifferent = cube.dimensions.any { dim ->
            dim != packedDimension && !value[dim].equals(sampleValue[dim])
        }
        if (dimensionDifferent) {
            return false
        }
        def valueIsNumber = value.value instanceof Number
        def sampleIsNumber = sampleValue.value instanceof Number
        if (valueIsNumber != sampleIsNumber) {
            def sampleType = sampleValue.value?.class?.simpleName
            def valueType = value.value?.class?.simpleName
            throw new Exception("Different value types within a packed message not supported: ${sampleType} != ${valueType}")
        }
        return true
    }

    protected void writePackedValues(OutputStream out, List<HypercubeValue> values, boolean last) {
        def message = createPackedCell(values)
        if (last) {
            message.last = last
        }
        writeMessage(out, message.build())
    }

    protected void addToPackedValues(OutputStream out, HypercubeValue value, boolean last) {
        if (currentValues.empty) {
            currentValues.add(value)
        } else if (canBeAppended(value)) {
            currentValues.add(value)
        } else {
            writePackedValues(out, currentValues, false)
            currentValues = [value]
        }
        if (last) {
            writePackedValues(out, currentValues, last)
        }
    }

    protected void writeCells(OutputStream out, Iterator<HypercubeValue> it) {
        while (it.hasNext()) {
            HypercubeValue value = it.next()
            if (packingEnabled) {
                addToPackedValues(out, value, !it.hasNext())
            } else {
                def message = createCell(value)
                def last = !it.hasNext()
                if (last) {
                    message.last = last
                }
                writeMessage(out, message.build())
            }
        }
    }

    protected Observation.Builder createCell(HypercubeValue value) {
        Observation.Builder builder = Observation.newBuilder()
        if (value.value != null) {
            if (value.value instanceof Number) {
                builder.numericValue = value.value as Double
            } else {
                builder.stringValue = value.value
            }
        }
        for (Dimension dim : cube.dimensions) {
            Object dimElement = value[dim]
            if (dim.density == Dimension.Density.SPARSE) {
                // Add the value element inline
                builder.addInlineDimensions(buildDimensionElement(dim, dimElement))
            } else {
                // Add index to footer element inline
                builder.addDimensionIndexes(determineFooterIndex(dim, dimElement))
            }
        }
        builder
    }

    protected PackedObservation.Builder createPackedCell(List<HypercubeValue> values) {
        PackedObservation.Builder builder = PackedObservation.newBuilder()
        assert values.size() > 0
        builder.numObervations = values.size()
        // make sure that packed dimension elements are added to the footer
        values.each { elements ->
            determineFooterIndex(packedDimension, elements[packedDimension])
        }
        if (dimensionElements[packedDimension].size() != values.size()) {
            throw new Exception("Not for every packed dimension element, there is an observation.")
        }
        // serialise packed observation values
        def packedValues = values.collect { it.value }
        if (packedValues.every { it instanceof Number }) {
            builder.addAllNumericValues(packedValues.collect { it as Double })
        } else {
            builder.addAllStringValues(packedValues.collect { it.toString() })
        }
        // serialise shared values of the packed message
        HypercubeValue sampleValue = values[0]
        for (Dimension dim : cube.dimensions) {
            if (dim != packedDimension) {
                Object dimElement = sampleValue[dim]
                if (dim.density == Dimension.Density.SPARSE) {
                    // Create a singleton array with the shared value of all observations
                    List<Object> objects = [sampleValue[dim]]
                    def dimensionElements = buildDimensionElements(dimensionDeclarations[dim], objects, false)
                    builder.addInlineDimensions(dimensionElements)
                } else {
                    // Add index to single footer element inline
                    builder.addDimensionIndexes(determineFooterIndex(dim, dimElement))
                }
            }
        }
        builder
    }

    static buildValue(Type type, Object value) {
        def builder = Value.newBuilder()
        switch (type) {
            case Type.TIMESTAMP:
                def timestampValue = TimestampValue.newBuilder()
                if (value == null) {
                    //
                } else if (value instanceof Date) {
                    timestampValue.val = value.time
                } else {
                    throw new Exception("Type not supported: ${value?.class?.simpleName}.")
                }
                builder.timestampValue = timestampValue.build()
                break
            case Type.DOUBLE:
                def doubleValue = DoubleValue.newBuilder()
                if (value instanceof Float) {
                    doubleValue.val = value.doubleValue()
                } else if (value instanceof Double) {
                    doubleValue.val = value.doubleValue()
                } else {
                    throw new Exception("Type not supported: ${value?.class?.simpleName}.")
                }
                builder.doubleValue = doubleValue.build()
                break
            case Type.INT:
                def intValue = IntValue.newBuilder()
                if (value == null) {
                    //
                } else if (value instanceof Number) {
                    intValue.val = value.longValue()
                } else {
                    Long id = value.getAt('id') as Long
                    if (id != null) {
                        intValue.val = id
                    }
                }
                builder.intValue = intValue.build()
                break
            case Type.STRING:
                def stringValue = StringValue.newBuilder()
                if (value != null) {
                    stringValue.val = value.toString()
                }
                builder.stringValue = stringValue.build()
                break
            default:
                throw new Exception("Type not supported: ${type.name()}.")
        }
        builder.build()
    }

    static addValue(DimElementField.Builder builder, Type type, Object value) {
        switch (type) {
            case Type.TIMESTAMP:
                def timestampValue = TimestampValue.newBuilder()
                if (value == null) {
                    //
                } else if (value instanceof Date) {
                    timestampValue.val = value.time
                } else {
                    throw new Exception("Type not supported for type ${type.name()}: ${value?.class?.simpleName}.")
                }
                builder.addTimestampValue timestampValue.build()
                break
            case Type.DOUBLE:
                def doubleValue = DoubleValue.newBuilder()
                if (value == null) {
                    // skip
                } else if (value instanceof Float) {
                    doubleValue.val = value.doubleValue()
                } else if (value instanceof Double) {
                    doubleValue.val = value.doubleValue()
                } else {
                    throw new Exception("Type not supported: ${value?.class?.simpleName} (value: ${value}, type: ${type.name()}).")
                }
                builder.addDoubleValue doubleValue.build()
                break
            case Type.INT:
                def intValue = IntValue.newBuilder()
                if (value == null) {
                    // skip
                } else if (value instanceof Number) {
                    intValue.val = value.longValue()
                } else {
                    Long id = value.getAt('id') as Long
                    if (id != null) {
                        intValue.val = id
                    }
                }
                builder.addIntValue intValue
                break
            case Type.STRING:
                def stringValue = StringValue.newBuilder()
                if (value != null) {
                    stringValue.val = value.toString()
                }
                builder.addStringValue stringValue.build()
                break
            default:
                throw new Exception("Type not supported: ${type.name()}.")
        }
        builder.build()
    }

    protected DimensionElement buildDimensionElement(Dimension dim, Object dimElement) {
        def builder = DimensionElement.newBuilder()
        DimensionDeclaration declaration = dimensionDeclarations[dim]
        if (declaration.type == Type.OBJECT) {
            declaration.fieldsList.each { field ->
                if (dim.class == StudyDimension) {
                    builder.addFields(buildValue(field.type, dimElement[field.name]))
                } else {
                    builder.addFields(buildValue(field.type, dimElement))
                }
            }
        } else {
            builder.addFields(buildValue(declaration.type, dimElement))
        }
        builder.build()
    }

    protected DimensionElements buildDimensionElements(
            DimensionDeclaration declaration,
            List<Object> objects,
            boolean perSample = false) {
        def elementsBuilder = DimensionElements.newBuilder()
        if (declaration.type == Type.OBJECT) {
            declaration.fieldsList.each { field ->
                def elementFieldBuilder = DimElementField.newBuilder()
                objects.each { elements ->
                    elements.each { element ->
                        addValue(elementFieldBuilder, field.type, element[field.name])
                    }
                }
                elementsBuilder.addFields(elementFieldBuilder)
                elementsBuilder.setPerSample(perSample)
            }
        } else {
            def elementFieldBuilder = DimElementField.newBuilder()
            objects.each { element ->
                addValue(elementFieldBuilder, declaration.type, element)
            }
            elementsBuilder.addFields(elementFieldBuilder)
            elementsBuilder.setPerSample(perSample)
        }
        elementsBuilder.build()
    }

    protected getFooter() {
        cube.dimensions.findAll({ it.density != Dimension.Density.SPARSE
        }).collect { dim ->
            buildDimensionElements(dimensionDeclarations[dim], dimensionElements[dim])
        }
    }

    protected Footer buildFooter() {
        Footer.newBuilder().addAllDimension(footer).build()
    }

    protected void writeFooter(OutputStream out) {
        writeMessage(out, buildFooter())
    }

}

