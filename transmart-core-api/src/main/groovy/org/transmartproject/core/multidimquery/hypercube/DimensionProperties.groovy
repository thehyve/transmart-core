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

    Type type

    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<Field> fields

    Boolean inline

    static DimensionProperties forDimension(Dimension dimension) {
        Type type
        List<Field> fields = null
        if (dimension.elementsSerializable) {
            // Sparse dimensions are inlined, dense dimensions are referred to by indexes
            // (referring to objects in the footer message).
            type = Type.forClass(dimension.elementType)
        } else {
            type = Type.MAP
            fields = dimension.elementFields.values().asList().collect {
                new Field(it.name, Type.forClass(it.type))
            }
        }
        new DimensionProperties(dimension.name, type, fields, dimension.density.isSparse)
    }

}
