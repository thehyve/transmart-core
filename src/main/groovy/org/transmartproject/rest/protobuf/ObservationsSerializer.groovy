package org.transmartproject.rest.protobuf

import com.google.protobuf.Empty
import com.google.protobuf.Message
import groovy.util.logging.Slf4j
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.db.multidimquery.AssayDimension
import org.transmartproject.db.multidimquery.BioMarkerDimension
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.multidimquery.HypercubeImpl
import org.transmartproject.db.multidimquery.HypercubeValueImpl
import org.transmartproject.db.multidimquery.ProjectionDimension
import org.transmartproject.db.multidimquery.StudyDimension
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

    protected List<Map<DimensionImpl, List<Object>>> inlineDimElements = new ArrayList<Map>()
    protected List<Map<DimensionImpl, Boolean>> packedDimFlagMap = new ArrayList<Map>()

    protected List<List<Long>> footerIndexes = []
    protected List<List<Object>> observationValues = []

    ObservationsSerializer(HypercubeImpl cube, Format format) {

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

    void writeEmptyMessage(OutputStream out) {
        if (format == Format.PROTOBUF) {
            Empty builder = new Empty()
            builder.writeDelimitedTo(out)
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
            if (dim.density == Dimension.Density.SPARSE) {
                builder.setInline(true)
            }
            if (packedDimFlagMap.any{it[dim]}) {
                builder.setPacked(true)
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

    protected boolean setupPackedValues() {

        def sparsePackableDims = cube.dimensions.findAll { dim ->
            dim.density == Dimension.Density.SPARSE && dim.packable.packable
        }
        Iterator<HypercubeValueImpl> it = cube.iterator
        while (it.hasNext()) {
            HypercubeValueImpl value = it.next()
            def dimIndexes = new ArrayList()
            extendListsForAnotherCubeValue()
            // determine DENSE dimensions footer indexes
            cube.dimensions.each { dim ->
                if (dim.density == Dimension.Density.DENSE) {
                    Object dimElement = value.getDimElement(dim)
                    dimIndexes << determineFooterIndex(dim, dimElement)
                }
            }

            if (!observationValues.size()) {
                // if it is first cube element, add to list, without checking
                observationValues.add([value.value])
                cube.dimensions.each { dim ->
                    if (dim.density == Dimension.Density.SPARSE) {
                        inlineDimElements[0][dim] = [value.getDimElement(dim)]
                    }
                }
            } else {
                // compare cube element with elements already added to list
                if(!compareAndPackIfPossible(dimIndexes, value, sparsePackableDims)){
                    addNotPackedElement(value, observationValues.size())
                }
            }
            footerIndexes << dimIndexes
        }
        if(!observationValues.any()) return false
        else return true
    }

    protected boolean compareAndPackIfPossible(List<Long> dimIndexes, HypercubeValueImpl value, List<DimensionImpl> sparsePackableDims) {
        footerIndexes.eachWithIndex { item, idx ->
            // compare types of values
            if (value.value.getClass() == observationValues[idx][0].getClass()) {
                // compare DENSE dimensions values via dimension elements indexes
                def diffDenseDimsNumber = countDifferentDenseDims(item, dimIndexes)
                // compare SPARSE dimensions values
                def diffSparseDims = getDifferentSparseDims(idx, value, sparsePackableDims)

                if (diffDenseDimsNumber == 0 && diffSparseDims.size() == 1) {
                    // values that differ in only one SPARSE dimension and have the same type can be packed
                    def differentDim = diffSparseDims.first()
                    packInlineDimsValues(idx, value, differentDim)
                    packedDimFlagMap[idx][differentDim] = true
                    observationValues[idx].add(value.value)
                    return true
                } else if (diffDenseDimsNumber == 1 && diffSparseDims.size() == 0) {
                    // values that differ in only one DENSE dimension and have the same type can be packed
                    def differentDim
                    item.eachWithIndex { elem, elemIdx ->
                        if (elem != dimIndexes[elemIdx]) {
                            differentDim = (new ArrayList<DimensionImpl>(dimensionElements*.key)).get(elemIdx)
                        }
                    }
                    packInlineDimsValues(idx, value)
                    packedDimFlagMap[idx][differentDim] = true
                    observationValues[idx].add(value.value)
                    return true
                }
            }
        }
        return false
    }

    protected void packInlineDimsValues(int idx, HypercubeValueImpl value, DimensionImpl excludedDim = null) {
        cube.dimensions.each { dim ->
            if (dim.density == Dimension.Density.SPARSE && dim != excludedDim) {
                inlineDimElements[idx][dim].add(value.getDimElement(dim))
                packedDimFlagMap[idx][dim] = true
            }
        }
    }

    protected ArrayList getDifferentSparseDims(int idx, HypercubeValueImpl value, List<DimensionImpl> sparsePackableDims) {
        def diffSparseDim = sparsePackableDims.findAll { dim ->
            value.getDimElement(dim) != inlineDimElements[idx][dim].first()
        }
        diffSparseDim
    }

    protected int countDifferentDenseDims(List<Long> item, List<Long> dimIndexes) {
        def diffDenseDimValue = dimIndexes - item.intersect(dimIndexes)
        diffDenseDimValue.size()
    }

    protected void addNotPackedElement(HypercubeValueImpl value, int idx) {
        cube.dimensions.each { dim ->
            if (dim.density == Dimension.Density.SPARSE){
                inlineDimElements[idx][dim] = [value.getDimElement(dim)]
            }
            packedDimFlagMap[idx][dim] = false
        }
        observationValues.add([value.value])
    }

    private void extendListsForAnotherCubeValue() {
        inlineDimElements.add(cube.dimensions.findAll{it.density == Dimension.Density.SPARSE}.collectEntries(new HashMap()) { [it, new ArrayList<Object>()] })
        packedDimFlagMap.add(cube.dimensions.collectEntries(new HashMap()) { [it, false] })
    }

    protected Header buildHeader() {
        Header.newBuilder().addAllDimensionDeclarations(dimensionsDefs).build()
    }

    protected void writeCells(OutputStream out) {
        observationValues.eachWithIndex{ value, idx ->
            def cell
            if (value.size() == 1) cell = createSingleCell(value.first(), idx)
            else cell = createPackedCell(value, idx)
            cell.last = (idx == (observationValues.size() - 1))
            writeMessage(out, cell.build())
        }
    }

    protected Observation.Builder createSingleCell(Object value, int valIndex) {
        Observation.Builder builder = Observation.newBuilder()

        if (value != null) {
            if (value instanceof Number) {
                builder.numericValue = value as Double
            } else {
                builder.stringValue = value
            }
        }
        inlineDimElements[valIndex].each {
            builder.addInlineDimensions(buildSparseCell(it.key, it.value.first()))
        }
        builder.addAllDimensionIndexes(footerIndexes[valIndex])
        builder
    }

    protected PackedObservation.Builder createPackedCell(List<Object> values, int valIndex, int numSamples = 1) {
        PackedObservation.Builder builder = PackedObservation.newBuilder()
        if (values.every { it instanceof Number }) {
            builder.addAllNumericValues(values)
        } else {
            builder.addAllStringValues(values)
        }

        inlineDimElements[valIndex].each { key, val ->
            builder.addInlineDimensions(buildDimensionElements(val, key))
            // TODO add support for multiple samples/timestamps
            builder.addNumSamples(numSamples)
        }
        builder.addAllDimensionIndexes(footerIndexes[valIndex])
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
            if(dim.class == StudyDimension) builder.addFields(buildValue(field, dimElement[field.name]))
            else builder.addFields(buildValue(field, dimElement))
        }
        builder.build()
    }

    protected DimensionElements buildDimensionElements(List<DimElementField> fields, DimensionImpl dim,
                                                       boolean perSample = false) {
        def elementsBuilder = DimensionElements.newBuilder()
        fields.each { field ->
            def elementFieldBuilder = DimElementField.newBuilder()
            dimensionElements[dim].each { elements ->
                elements.each { element ->
                    addValue(elementFieldBuilder, field, element.getAt(field.name))
                }
            }
            elementsBuilder.addFields(elementFieldBuilder)
            elementsBuilder.setPerSample(perSample)
        }
        elementsBuilder.build()
    }

    protected getFooter() {
        cube.dimensions.findAll({ it.density != Dimension.Density.SPARSE }).collect { dim ->
            def fields = dimensionFields[dim] ?: []
            buildDimensionElements(fields, dim)
        }
    }

    protected Footer buildFooter() {
        Footer.newBuilder().addAllDimension(footer).build()
    }

    protected Long determineFooterIndex(DimensionImpl dim, Object element) {
        if (dimensionElements[dim] == null) {
            dimensionElements[dim] = []
        }
        long index = dimensionElements[dim].indexOf(element)
        if (index == -1) {
            dimensionElements[dim].add(element)
            index = dimensionElements[dim].indexOf(element)
        }
        index
    }


    void write(OutputStream out) {
        begin(out)
        if (setupPackedValues()) {
            writeMessage(out, buildHeader())
            writeCells(out)
            writeMessage(out, buildFooter())
        } else {
            writeEmptyMessage(out)
        }
        end(out)
    }

}
