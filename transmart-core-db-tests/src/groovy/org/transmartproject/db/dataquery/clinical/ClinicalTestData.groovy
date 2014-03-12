package org.transmartproject.db.dataquery.clinical

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.TestDataHelper
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.i2b2data.I2b2Data
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.ontology.ConceptTestData
import org.transmartproject.db.querytool.QtQueryMaster

import static org.transmartproject.db.querytool.QueryResultData.createQueryResult
import static org.transmartproject.db.querytool.QueryResultData.getQueryResultFromMaster

class ClinicalTestData {

    I2b2Data i2b2Data
    ConceptTestData conceptData
    List<ObservationFact> facts

    @Lazy QtQueryMaster patientsQueryMaster = createQueryResult i2b2Data.patients

    static ClinicalTestData createDefault(ConceptTestData conceptData, I2b2Data i2b2Data) {
        def facts = createFacts(conceptData.conceptDimensions, i2b2Data.patients)
        new ClinicalTestData(i2b2Data: i2b2Data, conceptData: conceptData, facts: facts)
    }

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

    static List<ObservationFact> createFacts(List<ConceptDimension> concepts, List<Patient> patients)  {
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
    }

    void saveAll() {

        TestDataHelper.save([patientsQueryMaster])
        TestDataHelper.save facts
    }

}
