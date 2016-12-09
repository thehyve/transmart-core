package org.transmartproject.db.storage


import static org.transmartproject.db.TestDataHelper.save

/**
 * Created by piotrzakrzewski on 09/12/2016.
 */
class StorageTestData {

    List<StorageSystem> storageSystemList
    List<LinkedFileCollection> linkedFileCollectionList

    def saveAll() {
        save storageSystemList
        save linkedFileCollectionList
    }

    public static StorageTestData createDefault() {
        def storageTestData = new StorageTestData()
        storageTestData.linkedFileCollectionList = []
        storageTestData.storageSystemList = []
        def storageSystem = new StorageSystem()
        storageSystem.name = 'arvados keep at The Hyve'
        storageSystem.systemType = 'arvados'
        storageSystem.singleFileCollections = false
        storageSystem.systemVersion = 'v1'
        storageSystem.url = 'https://arvbox-pro-dev.thehyve.net'
        storageTestData.storageSystemList << storageSystem
        def linkedFileCollection = new LinkedFileCollection()
        linkedFileCollection.name = "1000 genemes VCFs"
        linkedFileCollection.sourceSystem = storageSystem
        linkedFileCollection.uuid = 'ys8ib-4zz18-cyw4o6pmrxrixnr'
        linkedFileCollection.study = 'STUDY_ID_1'
        storageTestData.linkedFileCollectionList << linkedFileCollection
        storageTestData
    }


}
