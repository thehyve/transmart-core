package org.transmartproject.db

import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.dataquery.highdim.mrna.MrnaTestData
import org.transmartproject.db.i2b2data.I2b2Data
import org.transmartproject.db.ontology.ConceptTestData
import org.transmartproject.db.ontology.I2b2

class TestData {

    ConceptTestData conceptData
    I2b2Data i2b2Data
    ClinicalTestData clinicalData
    MrnaTestData mrnaData

    private static TestData defaultInstance

    static TestData createDefault() {
        def conceptData = ConceptTestData.createDefault()
        def i2b2Data = I2b2Data.createDefault()
        def clinicalData = ClinicalTestData.createDefault(conceptData.i2b2List, i2b2Data.patients)

        def mrnaData = new MrnaTestData("2")

        defaultInstance = new TestData(
                conceptData: conceptData,
                i2b2Data: i2b2Data,
                clinicalData: clinicalData,
                mrnaData:  mrnaData,
        )
        defaultInstance
    }

    static getDefaultInstance() {
        defaultInstance
    }

    void saveAll() {
        conceptData?.saveAll()
        i2b2Data?.saveAll()
        clinicalData?.saveAll()
        mrnaData?.saveAll()
        mrnaData?.updateDoubleScaledValues()
    }

}
