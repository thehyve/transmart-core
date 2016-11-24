package org.transmartproject.rest.marshallers

import org.transmartproject.db.multidimquery.query.Field

class FieldSerializationHelper extends AbstractHalOrJsonSerializationHelper<Field> {

    final Class targetType = Field

    final String collectionName = 'fields'

    @Override
    Map<String, Object> convertToMap(Field field) {
        [dimension: field.dimension?.simpleName, fieldName: field.fieldName, type: field.type]
    }

}
