package org.transmartproject.db.ontology

import org.transmartproject.db.dataquery.highdim.HighDimTestData
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.i2b2data.I2b2Data
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.PatientDimension

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save
import static org.transmartproject.db.ontology.ConceptTestData.createI2b2
import static org.transmartproject.db.ontology.ConceptTestData.createTableAccess

class StudyTestData {

    I2b2Data i2b2Data = new I2b2Data('STUDY1')

    TableAccess tableAccess = createTableAccess(level: 0, fullName: '\\foo\\', name: 'foo',
            tableCode: 'i2b2 main', tableName: 'i2b2')

    List<I2b2> i2b2List = {
        [
                createI2b2(level: 1, fullName: '\\foo\\study1\\',         name: 'study1', cComment: 'trial:STUDY1'),
                createI2b2(level: 2, fullName: '\\foo\\study1\\bar\\',    name: 'bar',    cComment: 'trial:STUDY1'),

                createI2b2(level: 1, fullName: '\\foo\\study2\\',         name: 'study2', cComment: 'trial:STUDY2'),
                createI2b2(level: 2, fullName: '\\foo\\study2\\study1\\', name: 'study1', cComment: 'trial:STUDY2'),
        ]
    }()

    List<ConceptDimension> concepts = i2b2List.collect { I2b2 i2b2 ->
        new ConceptDimension(
                conceptPath: i2b2.fullName,
                conceptCode: i2b2.name /* this is arbitrary, doesn't have to the i2b2.name */
        )
    }

    // XXX: createTestPatients should be moved elsewhere
    List<PatientDimension> patients =  HighDimTestData.createTestPatients(3, -100)


    static ObservationFact createObservationFact(ConceptDimension concept,
                                                 PatientDimension patient,
                                                 Long encounterId,
                                                 Object value) {
        def of = new ObservationFact(
                encounterNum: encounterId as BigDecimal,
                providerId:   'fakeProviderId',
                modifierCd:   'fakeModifierCd',
                patient:      patient,
                conceptCode:  concept.conceptCode,
                startDate:    new Date(),
                instanceNum:  0,
                sourcesystemCd: 'STUDY1')

        if (value instanceof Number) {
            of.valueType = ObservationFact.TYPE_NUMBER
            of.textValue = 'E' //equal to
            of.numberValue = value as BigDecimal
        } else if (value != null) {
            of.valueType = ObservationFact.TYPE_TEXT
            of.textValue = value as String
        }

        of
    }

    List<ObservationFact> facts = {
        long encounterNum = -200
        def list1 = concepts[0..1].collect { ConceptDimension concept ->
            patients.collect { PatientDimension patient ->
                createObservationFact(concept, patient, --encounterNum,
                        "value for $concept.conceptCode/$patient.id")
            }
        }.inject([], { accum, factList -> accum + factList })

        list1 + [
                // missing fact for patients[0]
                createObservationFact(concepts[2], patients[1], --encounterNum, ''), //empty value
                createObservationFact(concepts[2], patients[2], --encounterNum, -45.42) //numeric value
        ]
    }()


    void saveAll() {
        i2b2Data.saveAll()

        save([tableAccess])
        save i2b2List
        save facts
    }

}
