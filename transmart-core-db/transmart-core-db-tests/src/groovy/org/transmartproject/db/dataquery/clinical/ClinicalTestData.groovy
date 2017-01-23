/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.dataquery.clinical

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.TestDataHelper
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.ontology.I2b2
import org.transmartproject.db.querytool.QtQueryMaster

import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.LEAF
import static org.transmartproject.db.querytool.QueryResultData.createQueryResult
import static org.transmartproject.db.querytool.QueryResultData.getQueryResultFromMaster

class ClinicalTestData {

    public static final long DUMMY_ENCOUNTER_ID = -300L
    List<Patient> patients
    List<ObservationFact> facts

    @Lazy QtQueryMaster patientsQueryMaster = createQueryResult patients

    QueryResult getQueryResult() {
        getQueryResultFromMaster patientsQueryMaster
    }

    static ClinicalTestData createDefault(List<I2b2> concepts, List<Patient> patients) {
        def facts = createDiagonalFacts(2, concepts, patients)
        new ClinicalTestData(patients: patients, facts: facts)
    }

    /**
     * Creates <code>count<code> facts from <code>count</code> leaf concepts
     * and <code>count</code> patients. Non-leaf concepts are ignored.
     *
     * All <code>count</code> patients and concepts will have exactly one
     * observation. The patients and concepts will be paired according to the
     * order they appear in their lists. The first patient will be paired with
     * the first concept, the second with the second, and so on.
     *
     * All the observations created will be numeric, with value
     * <code>10^i+1</code>, where <code>i = 0, 1, ... count - 1</code>.
     *
     * @param count number of facts to be created and expected minimum amount of patients and leaf concepts
     * @param concepts
     * @param patients
     * @return facts for leaf_concept[0] / patients[0], leaf_concept[1] / patients[1], etc...
     */
    static List<ObservationFact> createDiagonalFacts(int count, List<I2b2> concepts, List<Patient> patients) {

        assert patients.size() >= count

        def leafConceptsCodes = concepts.findAll {
            LEAF in it.visualAttributes
        } collect { it.code }

        assert leafConceptsCodes.size() >= count

        def facts = []
        for (int i = 0; i < count; i++) {
            facts << createObservationFact(leafConceptsCodes[i], patients[i],
                    DUMMY_ENCOUNTER_ID, Math.pow(10, i + 1))
        }
        facts
    }

    static List<ObservationFact> createDiagonalCategoricalFacts(
            int count, List<I2b2> concepts /* terminal */, List<Patient> patients) {

        assert patients.size() >= count
        assert concepts.size() > 0

        def map = [:]
        for (int i = 0; i < count; i++) {
            int j = i % concepts.size()
            assert LEAF in concepts[j].visualAttributes //leaf
            assert !concepts[j].metadata?.okToUseValues  //non-numeric

            map[patients[i]] = concepts[j]
        }

        createCategoricalFacts map
    }

    static List<ObservationFact> createCategoricalFacts(Map<Patient, I2b2> values) {
        values.collect { patient, i2b2 ->
            createObservationFact(
                    i2b2.code, patient, DUMMY_ENCOUNTER_ID, i2b2.name)
        }
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
                encounterNum:   encounterId as BigDecimal,
                providerId:     'fakeProviderId',
                modifierCd:     'fakeModifierCd',
                patient:        patient,
                conceptCode:    conceptCode,
                startDate:      new Date(),
                sourcesystemCd: patient.trial,
                instanceNum:    0)

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
