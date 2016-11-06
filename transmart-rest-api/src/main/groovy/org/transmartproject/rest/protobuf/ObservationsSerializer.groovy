package org.transmartproject.rest.protobuf

import com.google.common.collect.ImmutableList
import org.apache.commons.lang.StringUtils
import org.transmartproject.db.dataquery2.Dimension
import org.transmartproject.db.dataquery2.Hypercube
import org.transmartproject.db.dataquery2.HypercubeValue

/**
 * Created by piotrzakrzewski on 02/11/2016.
 */
public class ObservationsSerializer {

    public static final Set<String> DENSE_DIMENSIONS = []
    public static final Set<String> INLINE_DIMENSIONS = ["start_date"] //TODO: check spelling

    Hypercube cube
    List footerElements = new ArrayList()

    ObservationsSerializer(Hypercube cube) {
        this.cube = cube
    }

    def getDimensionsDefs() {
        ImmutableList<Dimension> dimensions = cube.getDimensions()

        def dimensionDeclarations = dimensions.collect() { dim ->
            def builder = ObservationsProto.DimensionDeclaration.newBuilder()
            String dimensionName = dim.toString()
            builder.setName(dimensionName)
            if (DENSE_DIMENSIONS.contains(dimensionName)) {
                builder.setIsDense(true)
            }
            if (INLINE_DIMENSIONS.contains(dimensionName)) {
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

    def getCells() {
        Iterator<HypercubeValue> it = cube.iterator
        List<Dimension> dims = cube.dimensions
        List<ObservationsProto.Observation> obsMessages = new ArrayList<>()
        while (it.hasNext()) {
            HypercubeValue value = it.next()
            ObservationsProto.Observation.Builder builder = ObservationsProto.Observation.newBuilder()
            builder.stringValue = value.value
            for (Dimension dim : dims) {
                Object dimElement = value.getDimElement(dim)
                if (dim.density == Dimension.Density.SPARSE) {
                    ObservationsProto.DimensionElements.Builder inlineDim = buildSparseCell(dimElement)
                    builder.addInlineDimensions(inlineDim)
                } else {
                    addDenseCell(builder, dim, dimElement)
                }
            }
            obsMessages.add(builder.build())
        }
        obsMessages
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

    int determineFooterIndex(Object dimElements) {
        if (!footerElements.contains(dimElements)) {
            footerElements.add(dimElements)
        }
        return footerElements.indexOf(dimElements)
    }

}
