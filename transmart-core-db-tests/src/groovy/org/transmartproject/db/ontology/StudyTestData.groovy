package org.transmartproject.db.ontology

import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.i2b2data.I2b2Data
import org.transmartproject.db.i2b2data.ObservationFact

import static org.transmartproject.db.TestDataHelper.save

class StudyTestData {

    I2b2Data i2b2Data = I2b2Data.createDefault()

    ConceptTestData conceptData = ConceptTestData.createDefault()

    List<ObservationFact> facts = {
        ClinicalTestData.createFacts(conceptData.conceptDimensions, i2b2Data.patients)
    }()

    List<I2b2> i2b2List = {
        conceptData.i2b2List
    }()

    TableAccess tableAccess = {
       conceptData.tableAccesses[0]
    }()

    List<ConceptDimension> concepts = {
       conceptData.conceptDimensions
    }()

    void saveAll() {
        i2b2Data.saveAll()
        conceptData.saveAll()
        save facts
    }

}
