package org.transmartproject.db.dataquery.clinical

import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.dataquery.highdim.HighDimTestData
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.ontology.ConceptTestData
import org.transmartproject.db.ontology.I2b2
import org.transmartproject.db.ontology.TableAccess
import org.transmartproject.db.querytool.QtQueryMaster

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save
import static org.transmartproject.db.ontology.ConceptTestData.createI2b2
import static org.transmartproject.db.querytool.QueryResultData.createQueryResult
import static org.transmartproject.db.querytool.QueryResultData.getQueryResultFromMaster

class ClinicalTestData {

    TableAccess tableAccess = ConceptTestData.createTableAccess(
            level:     0,
            fullName:  '\\foo\\',
            name:      'foo',
            tableCode: 'i2b2 main',
            tableName: 'i2b2')

    List<I2b2> i2b2Concepts = [
            createI2b2(level: 1, fullName: '\\foo\\concept 1\\', name: 'd1'), //not c, to test ordering
            createI2b2(level: 1, fullName: '\\foo\\concept 2\\', name: 'c2'),
            createI2b2(level: 1, fullName: '\\foo\\concept 3\\', name: 'c3'),
            createI2b2(level: 1, fullName: '\\foo\\concept 4\\', name: 'c4'),
    ]

    List<ConceptDimension> concepts = i2b2Concepts.collect { I2b2 i2b2 ->
        new ConceptDimension(
                conceptPath: i2b2.fullName,
                conceptCode: i2b2.name /* this is arbitrary, doesn't have to the i2b2.name */
        )
    }

    // XXX: createTestPatients should be moved elsewhere
    List<PatientDimension> patients =  HighDimTestData.createTestPatients(3, -100)

    @Lazy QtQueryMaster patientsQueryMaster = createQueryResult patients

    QueryResult getQueryResult() {
        getQueryResultFromMaster patientsQueryMaster
    }

    static ObservationFact createObservationFact(ConceptDimension concept,
                                                 PatientDimension patient,
                                                 Long encounterId,
                                                 Object value) {
        createObservationFact(concept.conceptCode, patient, encounterId, value)
    }

    static ObservationFact createObservationFact(String conceptCode,
                                                 PatientDimension patient,
                                                 Long encounterId,
                                                 Object value) {
        def of = new ObservationFact(
                encounterNum: encounterId as BigDecimal,
                providerId:   'fakeProviderId',
                modifierCd:   'fakeModifierCd',
                patient:      patient,
                conceptCode:  conceptCode,
                startDate:    new Date(),
                instanceNum:  0)

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
        save([tableAccess])
        save i2b2Concepts
        save concepts
        save patients
        save([patientsQueryMaster])
        save facts
    }

}
