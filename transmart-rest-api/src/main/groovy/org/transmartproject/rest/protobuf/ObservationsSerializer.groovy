package org.transmartproject.rest.protobuf

import com.google.common.collect.ImmutableList
import com.google.protobuf.util.JsonFormat
import org.apache.commons.lang.StringUtils
import org.transmartproject.db.dataquery2.Dimension
import org.transmartproject.db.dataquery2.Hypercube
import org.transmartproject.db.dataquery2.HypercubeValue

/**
 * Created by piotrzakrzewski on 02/11/2016.
 */
public class ObservationsSerializer {

    Hypercube cube
    JsonFormat.Printer jsonPrinter
    List footerElements = new ArrayList()

    ObservationsSerializer(Hypercube cube) {
        jsonPrinter = JsonFormat.printer()
        this.cube = cube
    }

    def getDimensionsDefs() {
        ImmutableList<Dimension> dimensions = cube.getDimensions()

        def dimensionDeclarations = dimensions.collect() { dim ->
            def builder = ObservationsProto.DimensionDeclaration.newBuilder()
            String dimensionName = dim.toString()
            builder.setName(dimensionName)
            if (dim.packable.packable) {
                builder.setIsDense(true)
            }
            if (dim.density == Dimension.Density.DENSE) {
                builder.setInline(true)
            }
            def properties = dim.properties
            def publicFacingFields = SerializableProperties.SERIALIZABLES.get(dimensionName)
            for (String publicField : publicFacingFields) {
                def fieldDefBuilder = ObservationsProto.FieldDefinition.newBuilder()
                fieldDefBuilder.setName(publicField)
                Class valueType = properties.get(publicField).getClass()
                Class targetType = HighDimBuilder.decideColumnValueType(valueType)
                if (targetType.equals(String)) {
                    fieldDefBuilder.setType(ObservationsProto.FieldDefinition.ColumnType.STRING)
                }
                builder.addFields(fieldDefBuilder)
            }
            builder
        }
        dimensionDeclarations
    }

    def writeHeader(BufferedWriter out, String format = "json") {
        def dimDefs = getDimensionsDefs()
        dimDefs.forEach() { dimDef ->
            jsonPrinter.appendTo(dimDef, out)
        }
    }

    def writeCells(BufferedWriter out) {
        Iterator<HypercubeValue> it = cube.iterator
        while (it.hasNext()) {
            HypercubeValue value = it.next()
            ObservationsProto.Observation.Builder builder = createCell(value)
            jsonPrinter.appendTo(builder, out)
        }
    }

    ObservationsProto.Observation.Builder createCell(HypercubeValue value) {
        ObservationsProto.Observation.Builder builder = ObservationsProto.Observation.newBuilder()
        builder.stringValue = value.value
        for (Dimension dim : cube.dimensions) {
            Object dimElement = value.getDimElement(dim)
            if (dim.density == Dimension.Density.SPARSE) {
                ObservationsProto.DimensionElements.Builder inlineDim = buildSparseCell(dimElement)
                builder.addInlineDimensions(inlineDim)
            } else {
                addDenseCell(builder, dim, dimElement)
            }
        }
        builder
    }

    private void addDenseCell(ObservationsProto.Observation.Builder builder, Dimension dim, Object dimElement) {
        ObservationsProto.DimensionCell.Builder dimBuilder = ObservationsProto.DimensionCell.newBuilder()
        int dimIndex = cube.dimensionsIndex.get(dim)
        dimBuilder.setDimensionIndex(dimIndex)
        int dimElIndex = determineFooterIndex(dimElement)
        dimBuilder.setValueIndex(dimElIndex)
        builder.addDimensions(dimBuilder)
    }

    private ObservationsProto.DimensionElements.Builder buildSparseCell(Object dimElement) {
        ObservationsProto.DimensionElements.Builder inlineDimBuilder = ObservationsProto.DimensionElements.newBuilder()
        Map<String, Object> props = dimElement.getProperties()
        for (String fieldName : props.keySet()) {
            ObservationsProto.DimensionElement.Builder dimElementBuilder = ObservationsProto.DimensionElement.newBuilder()
            String fieldVal = props.get(fieldName)
            if (StringUtils.isNotEmpty(fieldVal)) {
                dimElementBuilder.setStringValue(fieldVal)
                ObservationsProto.DimensionElement msg = dimElementBuilder.build()
                inlineDimBuilder.putFields(fieldName, msg)
            }
        }
        inlineDimBuilder
    }

    def getFooter() {
        footerElements.collect() { dimElement ->
            buildSparseCell(dimElement)
        }
    }

    def writeFooter(BufferedWriter out) {
        for (Object dimElement : footerElements) {
            jsonPrinter.appendTo(buildSparseCell(dimElement), out)
        }
    }

    int determineFooterIndex(Object dimElements) {
        if (!footerElements.contains(dimElements)) {
            footerElements.add(dimElements)
        }
        return footerElements.indexOf(dimElements)
    }

    void writeTo(OutputStream out, String format = "json") {
        Writer writer = new OutputStreamWriter(out)
        BufferedWriter bufferedWriter = new BufferedWriter(writer)
        writeHeader(bufferedWriter)
        writeCells(bufferedWriter)
        writeHeader(bufferedWriter)
        bufferedWriter.flush()
        bufferedWriter.close()
    }


}
