package org.transmartproject.rest.protobuf

import com.google.protobuf.Message
import groovy.util.logging.Slf4j
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.multidimquery.AssayDimension
import org.transmartproject.db.multidimquery.BioMarkerDimension
import org.transmartproject.db.multidimquery.ProjectionDimension
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.db.multidimquery.query.DimensionMetadata

import static com.google.protobuf.util.JsonFormat.*
import static org.transmartproject.rest.hypercubeProto.ObservationsProto.*
import static org.transmartproject.rest.hypercubeProto.ObservationsProto.FieldDefinition.*

/**
 * Created by piotrzakrzewski on 02/11/2016.
 */
@Slf4j
public class ObservationsSerializer {

    enum Format {
        JSON('application/json'),
        PROTOBUF('application/x-protobuf'),
        NONE('none')

        private String format

        Format(String format) {
            this.format = format
        }

        private static final Map<String, Format> mapping = Format.values().collectEntries {
            [(it.format): it]
        }

        public static Format from(String format) {
            if (mapping.containsKey(format)) {
                return mapping[format]
            } else {
                throw new Exception("Unknown format: ${format}")
            }
        }

        public String toString() {
            format
        }
    }

    protected Hypercube cube
    protected Printer jsonPrinter
    protected Writer writer
    protected Format format

    protected Map<Dimension, List<Object>> dimensionElements = [:]
    protected Map<Dimension, List<FieldDefinition>> dimensionFields = [:]

    ObservationsSerializer(Hypercube cube, Format format) {
        this.cube = cube
        if (format == Format.NONE) {
            throw new InvalidArgumentsException("No format selected.")
        } else if (format == Format.JSON) {
            jsonPrinter = printer()
        }
        this.format = format
    }

    protected boolean first = true

    protected void begin(OutputStream out) {
        first = true
        if (format == Format.JSON) {
            writer = new PrintWriter(new BufferedOutputStream(out))
            writer.print('[')
        }
    }

    protected void writeMessage(OutputStream out, Message message) {
        if (format == Format.JSON) {
            if (!first) {
                writer.print(', ')
            }
            jsonPrinter.appendTo(message, writer)
        } else {
            message.writeDelimitedTo(out)
        }
        if (first) {
            first = false
        }
    }

    protected void end(OutputStream out) {
        if (format == Format.JSON) {
            writer.print(']')
            writer.flush()
        } else {
            out.flush()
        }
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

    protected getDimensionsDefs() {
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
            switch(dim.class) {
                case BioMarkerDimension:
                    break
                case AssayDimension:
                    break
                case ProjectionDimension:
                    break
                default:
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
                    break
            }
            dimensionFields[dim] = fields

            builder.build()
        }
        dimensionDeclarations
    }

    protected Header buildHeader() {
        Header.newBuilder().addAllDimensionDeclarations(dimensionsDefs).build()
    }

    protected void writeCells(OutputStream out) {
        Iterator<HypercubeValue> it = cube.iterator
        while (it.hasNext()) {
            HypercubeValue value = it.next()
            def cell = createCell(value)
            cell.last = !it.hasNext()
            writeMessage(out, cell.build())
        }
    }

    protected Observation.Builder createCell(HypercubeValue value) {
        Observation.Builder builder = Observation.newBuilder()
        if (value.value instanceof Number) {
            builder.numericValue = value.value
        } else {
            builder.stringValue = value.value
        }
        for (Dimension dim : cube.dimensions) {
            Object dimElement = value.getAt(dim)
            if (dim.density == Dimension.Density.SPARSE) {
                builder.addInlineDimensions(buildSparseCell(dim, dimElement))
            } else {
                builder.addDimensionIndexes(determineFooterIndex(dim, dimElement))
            }
        }
        builder
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

    protected DimensionElement buildSparseCell(Dimension dim, Object dimElement) {
        def builder = DimensionElement.newBuilder()
        for (FieldDefinition field: dimensionFields[dim]) {
            builder.addFields(buildValue(field, dimElement.getAt(field.name)))
        }
        builder.build()
    }

    protected getFooter() {
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

    protected Footer buildFooter() {
        Footer.newBuilder().addAllDimension(footer).build()
    }

    protected int determineFooterIndex(Dimension dim, Object element) {
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

    void write(OutputStream out) {
        begin(out)
        writeMessage(out, buildHeader())
        writeCells(out)
        writeMessage(out, buildFooter())
        end(out)
    }

}
