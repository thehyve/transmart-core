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

import com.google.common.collect.ImmutableSet
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.TestDataHelper
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.i2b2data.TrialVisit
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.i2b2data.VisitDimension
import org.transmartproject.db.ontology.I2b2
import org.transmartproject.db.querytool.QtQueryMaster
import org.codehaus.groovy.runtime.InvokerHelper

import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.LEAF
import static org.transmartproject.db.querytool.QueryResultData.createQueryResult
import static org.transmartproject.db.querytool.QueryResultData.getQueryResultFromMaster

class ClinicalTestData {

    public static final long DUMMY_ENCOUNTER_ID = -1
    public static final long DUMMY_INSTANCE_ID = 1
    List<Patient> patients
    List<VisitDimension> visits
    List<ObservationFact> facts
    List<ObservationFact> longitudinalClinicalFacts
    List<ObservationFact> sampleClinicalFacts
    List<ObservationFact> ehrClinicalFacts

    @Lazy
    QtQueryMaster patientsQueryMaster = createQueryResult patients

    // defaultTrialVisit should be test scoped, but I haven't found an automatic way to do that. For now we just
    // clear the field after every test.
    static private _defaultTrialVisit = null
    static private synchronized getDefaultTrialVisit() {
        if(!_defaultTrialVisit) {
            _defaultTrialVisit = createTrialVisit("days", 3, 'label')
        }
        _defaultTrialVisit
    }

    /** Any tests that use ClinicalTestData must call reset() in their cleanup method */
    static synchronized reset() {
        _defaultTrialVisit = null
    }

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
                                                 Object value,
                                                 int instanceNum = DUMMY_INSTANCE_ID,
                                                 TrialVisit trialVisit = defaultTrialVisit){

        def of = new ObservationFact(
                encounterNum: encounterId as BigDecimal,
                providerId: 'fakeProviderId',
                modifierCd: '@',
                patient: patient,
                conceptCode: conceptCode,
                startDate: new Date(),
                sourcesystemCd: patient.trial,
                instanceNum: instanceNum,
        )
        trialVisit.addToObservationFacts(of)

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

    static ObservationFact extendObservationFact(ObservationFact basicObservationFact,
                                                 int instanceNum,
                                                 String modifierCd,
                                                 Object value) {

        def extendedFact = new ObservationFact()
        InvokerHelper.setProperties(extendedFact, basicObservationFact.properties)

        extendedFact.setInstanceNum(instanceNum)
        extendedFact.setModifierCd(modifierCd)

        if (value instanceof Number) {
            extendedFact.valueType = ObservationFact.TYPE_NUMBER
            extendedFact.textValue = 'E' //equal to
            extendedFact.numberValue = value as BigDecimal
        } else if (value != null) {
            extendedFact.valueType = ObservationFact.TYPE_TEXT
            extendedFact.textValue = value as String
        }

        extendedFact
    }

    static List<ObservationFact> createFacts(List<ConceptDimension> concepts, List<Patient> patients) {
        long encounterNum = -200
        def list1 = concepts[0..1].collect { ConceptDimension concept ->
            patients.collect { PatientDimension patient ->
                createObservationFact(concept, patient, --encounterNum,
                        "value for $concept.conceptCode/$patient.id")
            }
        }.inject([], { accum, factList -> accum + factList })

        list1 + [
                // missing fact for patients[0]
                createObservationFact(concepts[2], patients[1], --encounterNum, '', ), //empty value
                createObservationFact(concepts[2], patients[2], --encounterNum, -45.42) //numeric value
        ]
    }

    static List<ObservationFact> createLongitudinalFacts(ConceptDimension concept, List<Patient> patients){

        [ createObservationFact(concept.conceptCode, patients[0], DUMMY_ENCOUNTER_ID, '', 1, createTrialVisit('days', 2, 'label_1')),
          createObservationFact(concept.conceptCode, patients[1], DUMMY_ENCOUNTER_ID, '', 1, createTrialVisit('days', 4, 'label_2')),
          createObservationFact(concept.conceptCode, patients[2], DUMMY_ENCOUNTER_ID, '', 1, createTrialVisit('weeks', 2, 'label_3')) ]
    }

    static List<ObservationFact> createSampleFacts(ConceptDimension concept, List<Patient> patients){

        def fact = createObservationFact(concept.conceptCode, patients[2], DUMMY_ENCOUNTER_ID, '', 1, createTrialVisit('days', 2, 'label_1'))
        String modifierCd = 'TEST:TISSUETYPE'

        [ fact,
          extendObservationFact(fact, 1, modifierCd, 'CONNECTIVE TISSUE'),
          extendObservationFact(fact, 2, modifierCd, 'MUSCLE TISSUE')      ]
    }

    static List<ObservationFact> createEhrFacts(ConceptDimension concept, List<VisitDimension> visits){
        def ehrFacts = []
        for (int i = 0; i < visits.size(); i++) {
            ehrFacts << createObservationFact(concept.conceptCode, visits[i].patient, visits[i].encounterNum, -45.42)
        }
        ehrFacts
    }

    static TrialVisit createTrialVisit(String relTimeUnit, int relTime, String studyLabel, Study study) {
        def tv = new TrialVisit(
                relTimeUnit: relTimeUnit,
                relTime: relTime,
                relTimeLabel: studyLabel,
        )
        study.addToTrialVisits(tv)
        tv
    }

    static TrialVisit createDefaultTrialVisit(String relTimeUnit, int relTime, String studyLabel) {


        def study = new Study(
                name: "study_name"
        )

        def tv = new TrialVisit(
                relTimeUnit: relTimeUnit,
                relTime: relTime,
                relTimeLabel: studyLabel,
        )
        study.addToTrialVisits(tv)
        tv
    }


    static List<VisitDimension> createTestVisit(int n, PatientDimension patient, Date startDate, Date endDate) {
        (1..n).collect { int i ->
            def visit = new VisitDimension(
                    patient: patient,
                    encounterNum: DUMMY_ENCOUNTER_ID + (i +1),
                    startDate: startDate + (10*i),
                    endDate: endDate + (10*i),
//                activeStatusCd: ActiveStatus.ACTIVE,
//                inoutCd: 'inout_cd',
//                locationCd: 'location_cd',
//                locationPath: 'location_path',
//                lengthOfStay: 1,
//                visitBlob: 'visit_blob',
//                updateDate: new Date(),
//                downloadDate: new Date(),
//                importDate: new Date(),
//                sourcesystemCd: 'sourcesystem_cd',
//                uploadId: 'upload_id'
            )
            visit
        }
    }

    void saveAll() {
        TestDataHelper.save([patientsQueryMaster])
        TestDataHelper.save visits
        TestDataHelper.save facts
        TestDataHelper.save sampleClinicalFacts
        TestDataHelper.save longitudinalClinicalFacts
        TestDataHelper.save ehrClinicalFacts
    }

}
