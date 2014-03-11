package org.transmartproject.db.ontology

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.i2b2data.I2b2Data

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save
import static org.transmartproject.db.ontology.ConceptTestData.createConcept
import static org.transmartproject.db.ontology.ConceptTestData.createTableAccess

class StudyTestData {

    I2b2Data i2b2Data = new I2b2Data('STUDY1')

    TableAccess tableAccess = createTableAccess(level: 0, fullName: '\\foo\\', name: 'foo',
            tableCode: 'i2b2 main', tableName: 'i2b2')

    List<I2b2> i2b2List = {
        [
                createConcept(level: 1, fullName: '\\foo\\study1\\',         name: 'study1', cComment: 'trial:STUDY1'),
                createConcept(level: 2, fullName: '\\foo\\study1\\bar\\',    name: 'bar',    cComment: 'trial:STUDY1'),

                createConcept(level: 1, fullName: '\\foo\\study2\\',         name: 'study2', cComment: 'trial:STUDY2'),
                createConcept(level: 2, fullName: '\\foo\\study2\\study1\\', name: 'study1', cComment: 'trial:STUDY2'),
        ]
    }()

    void saveAll() {
        i2b2Data.saveAll()

        save([tableAccess])
        save i2b2List

        saveExtraData()
    }

    /**
     * Saves extra data outside the I2B2/TableAccess tables, but required for some tests
     */
    void saveExtraData() {
        //concept dimensions
        save ConceptTestData.createConceptDimensions(i2b2List)

        //patients
        List<Patient> patients = I2b2Data.createTestPatients(3, -1000, 'SAMPLE_TRIAL')
        save patients

        //observations
        def observations = []
        observations << ClinicalTestData.createObservationFact(i2b2List[1].code, patients[0], 2, 3)
        observations << ClinicalTestData.createObservationFact(i2b2List[1].code, patients[1], 4, 5)
        save observations
    }

}
