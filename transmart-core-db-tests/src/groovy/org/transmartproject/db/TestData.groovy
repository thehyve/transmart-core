package org.transmartproject.db

import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.i2b2data.I2b2Data
import org.transmartproject.db.ontology.ConceptTestData

class TestData {

    ConceptTestData conceptData
    I2b2Data i2b2Data
    ClinicalTestData clinicalData

    static TestData createDefault() {
        def concept = ConceptTestData.createDefault()
        def i2b2 = I2b2Data.createDefault()
        def clinical = ClinicalTestData.createDefault(concept, i2b2)

        new TestData(conceptData: concept, i2b2Data: i2b2, clinicalData: clinical)
    }

    void saveAll() {
        conceptData?.saveAll()
        i2b2Data?.saveAll()
        clinicalData?.saveAll()
    }

}
