package org.transmartproject.db

import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.i2b2data.I2b2Data
import org.transmartproject.db.ontology.ConceptTestData

class TestData {

    ConceptTestData concept
    I2b2Data i2b2
    ClinicalTestData clinical

    static TestData createDefault() {
        def concept = ConceptTestData.createDefault()
        def i2b2 = I2b2Data.createDefault()
        def clinical = ClinicalTestData.createDefault(concept, i2b2)

        new TestData(concept: concept, i2b2: i2b2, clinical: clinical)
    }

    void saveAll() {
        concept?.saveAll()
        i2b2?.saveAll()
        clinical?.saveAll()
    }

}
