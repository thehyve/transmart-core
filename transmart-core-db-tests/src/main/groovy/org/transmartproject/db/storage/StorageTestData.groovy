package org.transmartproject.db.storage

import org.transmartproject.db.StudyTestData
import org.transmartproject.db.i2b2data.Study
import static org.transmartproject.db.TestDataHelper.save

/**
 * Created by piotrzakrzewski on 09/12/2016.
 */
class StorageTestData {

    List<StorageSystem> storageSystemList
    List<LinkedFileCollection> linkedFileCollectionList
    List<Study> studies

    def saveAll() {
        save studies
        save storageSystemList
        save linkedFileCollectionList
    }

    public static StorageTestData createDefault() {
        def storageTestData = new StorageTestData()
        def study1 = StudyTestData.createStudy "storage_study"
        def study2 = StudyTestData.createStudy "storage_study2"
        storageTestData.studies = []
        storageTestData.studies << study1
        storageTestData.studies << study2
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
        linkedFileCollection.study = study1
        storageTestData.linkedFileCollectionList << linkedFileCollection
        def deleteMesystem = new StorageSystem()
        deleteMesystem.name = "delete_me"
        deleteMesystem.systemType = 'oracle'
        deleteMesystem.singleFileCollections = true
        deleteMesystem.systemVersion = '11g'
        deleteMesystem.url = 'http://oracle.someco.com:6726'
        storageTestData.storageSystemList << deleteMesystem
        storageTestData
    }


}
