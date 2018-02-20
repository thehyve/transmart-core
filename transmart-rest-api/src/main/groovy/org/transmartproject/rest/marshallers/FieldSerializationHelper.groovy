package org.transmartproject.rest.marshallers

import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.query.Field

@CompileStatic
class FieldSerializationHelper extends AbstractHalOrJsonSerializationHelper<Field> {

    final Class targetType = Field

    final String collectionName = 'fields'

    @Override
    Map<String, Object> convertToMap(Field field) {
        (Map<String, Object>)[dimension: field.dimension, fieldName: field.fieldName, type: field.type.name()]
    }

}
