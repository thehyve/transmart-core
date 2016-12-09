package org.transmartproject.rest.marshallers

import org.transmartproject.db.storage.LinkedFileCollection

/**
 * Created by piotrzakrzewski on 09/12/2016.
 */
class LinkedFileCollectionSerializationHelper extends AbstractHalOrJsonSerializationHelper<LinkedFileCollection>{

    final Class targetType = LinkedFileCollection

    final String collectionName = 'fileCollections'

    @Override
    Map<String, Object> convertToMap(LinkedFileCollection linkedFileCollection) {
        ['sourceSystemId':linkedFileCollection.sourceSystem?.id,
        'name':linkedFileCollection.name,
        'studyId':linkedFileCollection.study,
        'uuid':linkedFileCollection.uuid]
    }

}
