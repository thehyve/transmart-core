package org.transmartproject.db.dataquery.clinical

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.TestDataHelper
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.ontology.I2b2
import org.transmartproject.db.querytool.QtQueryMaster

import static org.transmartproject.db.querytool.QueryResultData.createQueryResult
import static org.transmartproject.db.querytool.QueryResultData.getQueryResultFromMaster

class ClinicalTestData {

    List<Patient> patients
    List<ObservationFact> facts

    @Lazy QtQueryMaster patientsQueryMaster = createQueryResult patients

    QueryResult getQueryResult() {
        getQueryResultFromMaster patientsQueryMaster
    }

    static ClinicalTestData createDefault(List<I2b2> concepts, List<Patient> patients) {
        def facts = createFacts(2, concepts, patients)
        new ClinicalTestData(patients: patients, facts: facts)
    }

    /**
     * @param count number of facts to be created and expected minimum amount of patients and leaf concepts
     * @param concepts
     * @param patients
     * @return facts for leaf_concept[0] / patients[0], leaf_concept[1] / patients[1], etc...
     */
    static List<ObservationFact> createFacts(int count, List<I2b2> concepts, List<Patient> patients) {

        assert patients.size() >= count

        def leafConceptsCodes = concepts.findAll {
            OntologyTerm.VisualAttributes.LEAF in it.visualAttributes
        } collect { it.code }

        assert leafConceptsCodes.size() >= count

        def facts = []
        for (int i = 0; i < count; i++) {
            facts << createObservationFact(leafConceptsCodes[i], patients[i], -300, Math.pow(10, i+1))
        }
        facts
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
