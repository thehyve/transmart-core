package org.transmartproject.core.multidimquery.hypercube

import com.fasterxml.jackson.annotation.JsonInclude
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode

/**
 * Contains information about a dimension
 */
@CompileStatic
@Canonical
@EqualsAndHashCode(includes = 'name')
class DimensionProperties {

    String name

    DimensionType dimensionType

    Integer sortIndex

    ValueType valueType

    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<Field> fields

    Boolean inline

    static DimensionProperties forDimension(Dimension dimension) {
        ValueType valueType
        List<Field> fields = null
        if (dimension.elementsSerializable) {
            // Sparse dimensions are inlined, dense dimensions are referred to by indexes
            // (referring to objects in the footer message).
            valueType = ValueType.forClass(dimension.elementType)
        } else {
            valueType = ValueType.MAP
            fields = dimension.elementFields.values().asList().collect {
                new Field(it.name, ValueType.forClass(it.type))
            }
        }
        new DimensionProperties(dimension.name, dimension.dimensionType, dimension.sortIndex, valueType, fields, dimension.density.isSparse)
    }

}
