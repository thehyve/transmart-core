package org.transmartproject.rest.protobuf

import com.google.common.collect.ImmutableList
import groovy.transform.CompileStatic
import org.transmartproject.db.dataquery2.Dimension
import org.transmartproject.db.dataquery2.Hypercube
import org.transmartproject.db.dataquery2.HypercubeValue

/**
 * Created by piotrzakrzewski on 02/11/2016.
 */
public class ObservationsSerializer {

    public static final Set<String> DENSE_DIMENSIONS = []
    public static final Set<String> INLINE_DIMENSIONS = ["start_date"] //TODO: check spelling

    def getDimensionsDefs(Hypercube hypercube) {
        ImmutableList<Dimension> dimensions = hypercube.getDimensions()

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

    @CompileStatic
    def getCells(Hypercube cube) {
        Iterator<HypercubeValue> it = cube.iterator
        List<Dimension> dims = cube.dimensions
        while (it.hasNext()) {
            HypercubeValue value = it.next()
            def dimElements = dims.collect() { dim ->
                value.getDimElement(dim)
            }

        }
    }


}
