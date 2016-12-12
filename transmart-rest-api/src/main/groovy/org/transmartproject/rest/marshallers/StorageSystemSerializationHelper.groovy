package org.transmartproject.rest.marshallers

import org.transmartproject.db.storage.LinkedFileCollection
import org.transmartproject.db.storage.StorageSystem

/**
 * Created by piotrzakrzewski on 12/12/2016.
 */
class StorageSystemSerializationHelper extends AbstractHalOrJsonSerializationHelper<StorageSystem>{

    final Class targetType = StorageSystem

    final String collectionName = 'fileCollections'

    @Override
    Map<String, Object> convertToMap(StorageSystem storageSystem) {
        ['id':storageSystem.id,
                'name':storageSystem.name,
         'systemType':storageSystem.systemType,
         'url':storageSystem.url,
         'systemVersion':storageSystem.systemVersion,
         'singleFileCollections':storageSystem.singleFileCollections,
        ]
    }
}
