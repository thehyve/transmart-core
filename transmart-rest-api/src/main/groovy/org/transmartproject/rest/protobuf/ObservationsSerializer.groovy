package org.transmartproject.rest.protobuf

import com.google.protobuf.util.JsonFormat
import groovy.util.logging.Slf4j
import org.transmartproject.db.dataquery2.Dimension
import org.transmartproject.db.dataquery2.Hypercube
import org.transmartproject.db.dataquery2.HypercubeValue
import org.transmartproject.db.dataquery2.query.DimensionMetadata

import static org.transmartproject.rest.hypercubeProto.ObservationsProto.*
import static org.transmartproject.rest.hypercubeProto.ObservationsProto.FieldDefinition.*

/**
 * Created by piotrzakrzewski on 02/11/2016.
 */
@Slf4j
public class ObservationsSerializer {

    Hypercube cube
    JsonFormat.Printer jsonPrinter
    Map<Dimension, List<Object>> dimensionElements = [:]
    Map<Dimension, List<FieldDefinition>> dimensionFields = [:]

    ObservationsSerializer(Hypercube cube) {
        jsonPrinter = JsonFormat.printer()
        this.cube = cube
    }

    static ColumnType getFieldType(Class type) {
        if (Float.isAssignableFrom(type)) {
            return ColumnType.DOUBLE
        } else if (Double.isAssignableFrom(type)) {
            return ColumnType.DOUBLE
        } else if (Number.isAssignableFrom(type)) {
            return ColumnType.INT
        } else if (Date.isAssignableFrom(type)) {
            return ColumnType.TIMESTAMP
        } else if (String.isAssignableFrom(type)) {
            return ColumnType.STRING
        } else {
            // refer to objects by their identifier
            return ColumnType.INT
        }
    }

    def getDimensionsDefs() {
        def dimensionDeclarations = cube.dimensions.collect { dim ->
            def builder = DimensionDeclaration.newBuilder()
            String dimensionName = dim.toString()
            builder.setName(dimensionName)
            if (dim.packable.packable) {
                builder.setIsDense(true)
            }
            if (dim.density == Dimension.Density.DENSE) {
                builder.setInline(true)
            }
            def publicFacingFields = SerializableProperties.SERIALIZABLES.get(dimensionName)
            def fields = []
            def metadata = DimensionMetadata.forDimension(dim.class)
            metadata.fields.each { field ->
                if (field.fieldName in publicFacingFields) {
                    Class valueType = metadata.fieldTypes[field.fieldName]
                    def fieldDef = FieldDefinition.newBuilder()
                            .setName(field.fieldName)
                            .setType(getFieldType(valueType))
                            .build()
                    fields << fieldDef
                    builder.addFields(fieldDef)
                }
            }
            dimensionFields[dim] = fields
            builder.build()
        }
        dimensionDeclarations
    }

    def writeHeader(BufferedWriter out, String format = "json") {
        def header = Header.newBuilder().addAllDimensionDeclarations(dimensionsDefs)
        jsonPrinter. appendTo(header, out)
    }

    def writeCells(BufferedWriter out) {
        Iterator<HypercubeValue> it = cube.iterator
        while (it.hasNext()) {
            HypercubeValue value = it.next()
            Observation observation = createCell(value)
            jsonPrinter.appendTo(observation, out)
        }
    }

    Observation createCell(HypercubeValue value) {
        Observation.Builder builder = Observation.newBuilder()
        if (value.value instanceof Number) {
            builder.numericValue = value.value
        } else {
            builder.stringValue = value.value
        }
        for (Dimension dim : cube.dimensions) {
            Object dimElement = value.getDimElement(dim)
            if (dim.density == Dimension.Density.SPARSE) {
                builder.addInlineDimensions(buildSparseCell(dim, dimElement))
            } else {
                builder.addDimensionIndexes(determineFooterIndex(dim, dimElement))
            }
        }
        builder.build()
    }

    static buildValue(FieldDefinition field, Object value) {
        def builder = Value.newBuilder()
        switch (field.type) {
            case ColumnType.TIMESTAMP:
                def timestampValue = TimestampValue.newBuilder()
                if (value == null) {
                    //
                } else if (value instanceof Date) {
                    timestampValue.val = value.time
                } else {
                    throw new Exception("Type not supported.")
                }
                builder.timestampValue = timestampValue.build()
                break
            case ColumnType.DOUBLE:
                def doubleValue = DoubleValue.newBuilder()
                if (value instanceof Float) {
                    doubleValue.val = value.doubleValue()
                } else if (value instanceof Double) {
                    doubleValue.val = value.doubleValue()
                } else {
                    throw new Exception("Type not supported.")
                }
                builder.doubleValue = doubleValue.build()
                break
            case ColumnType.INT:
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
            case ColumnType.STRING:
                def stringValue = StringValue.newBuilder()
                if (value != null) {
                    stringValue.val = value.toString()
                }
                builder.stringValue = stringValue.build()
                break
            default:
                throw new Exception("Type not supported.")
        }
        builder.build()
    }

    static addValue(DimElementField.Builder builder, FieldDefinition field, Object value) {
        switch (field.type) {
            case ColumnType.TIMESTAMP:
                def timestampValue = TimestampValue.newBuilder()
                if (value == null) {
                    //
                } else if (value instanceof Date) {
                    timestampValue.val = value.time
                } else {
                    throw new Exception("Type not supported.")
                }
                builder.addTimestampValue timestampValue.build()
                break
            case ColumnType.DOUBLE:
                def doubleValue = DoubleValue.newBuilder()
                if (value instanceof Float) {
                    doubleValue.val = value.doubleValue()
                } else if (value instanceof Double) {
                    doubleValue.val = value.doubleValue()
                } else {
                    throw new Exception("Type not supported.")
                }
                builder.addDoubleValue doubleValue.build()
                break
            case ColumnType.INT:
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
            case ColumnType.STRING:
                def stringValue = StringValue.newBuilder()
                if (value != null) {
                    stringValue.val = value.toString()
                }
                builder.addStringValue stringValue.build()
                break
            default:
                throw new Exception("Type not supported.")
        }
        builder.build()
    }

    private DimensionElement buildSparseCell(Dimension dim, Object dimElement) {
        def builder = DimensionElement.newBuilder()
        for (FieldDefinition field: dimensionFields[dim]) {
            builder.addFields(buildValue(field, dimElement.getAt(field.name)))
        }
        builder.build()
    }

    def getFooter() {
        cube.dimensions.findAll({ it.density != Dimension.Density.SPARSE }).collect { dim ->
            def fields = dimensionFields[dim] ?: []
            def elementsBuilder = DimensionElements.newBuilder()
            fields.each { field ->
                def elementFieldBuilder = DimElementField.newBuilder()
                dimensionElements[dim].each { elements ->
                    elements.each { element ->
                        addValue(elementFieldBuilder, field, element.getAt(field.name))
                    }
                }
                elementsBuilder.addFields(elementFieldBuilder)
            }
            elementsBuilder.build()
        }
    }

    def writeFooter(BufferedWriter out) {
        def footer = Footer.newBuilder().addAllDimension(footer)
        jsonPrinter.appendTo(footer, out)
    }

    int determineFooterIndex(Dimension dim, Object element) {
        if (dimensionElements[dim] == null) {
            dimensionElements[dim] = []
        }
        int index = dimensionElements[dim].indexOf(element)
        if (index == -1) {
            dimensionElements[dim].add(element)
            index = dimensionElements[dim].indexOf(element)
        }
        index
    }

    void writeTo(OutputStream out, String format = "json") {
        Writer writer = new OutputStreamWriter(out)
        BufferedWriter bufferedWriter = new BufferedWriter(writer)
        writeHeader(bufferedWriter)
        writeCells(bufferedWriter)
        writeFooter(bufferedWriter)
        bufferedWriter.flush()
        bufferedWriter.close()
    }

}
