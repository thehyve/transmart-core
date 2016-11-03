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

import org.codehaus.groovy.runtime.InvokerHelper
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.StudyTestData
import org.transmartproject.db.TestDataHelper
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.i2b2data.TrialVisit
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.i2b2data.VisitDimension
import org.transmartproject.db.ontology.I2b2
import org.transmartproject.db.querytool.QtQueryMaster

import java.text.SimpleDateFormat

import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.LEAF
import static org.transmartproject.db.querytool.QueryResultData.createQueryResult
import static org.transmartproject.db.querytool.QueryResultData.getQueryResultFromMaster

class ClinicalTestData {

    public static final long DUMMY_ENCOUNTER_ID = -1
    public static final long DUMMY_INSTANCE_ID = 1
    List<PatientDimension>  patients
    List<VisitDimension>    visits
    Study                   study
    List<ObservationFact>   facts
    Study                   longitudinalStudy
    List<ObservationFact>   longitudinalClinicalFacts
    Study                   sampleStudy
    List<ObservationFact>   sampleClinicalFacts
    Study                   ehrStudy
    List<ObservationFact>   ehrClinicalFacts

    @Lazy
    QtQueryMaster patientsQueryMaster = createQueryResult patients

    // defaultTrialVisit should be test scoped, but I haven't found an automatic way to do that. For now we just
    // clear the field after every test.
    static private _defaultTrialVisit = null
    static private synchronized getDefaultTrialVisit() {
        if(!_defaultTrialVisit) {
            _defaultTrialVisit = createDefaultTrialVisit("day", 3)
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

    static ClinicalTestData createDefault(List<I2b2> concepts, List<PatientDimension> patients) {
        def facts = createDiagonalFacts(2, concepts, patients)
        new ClinicalTestData(patients: patients, facts: facts)
    }

    static ClinicalTestData createHypercubeDefault(List<ConceptDimension> conceptDims,
                                                   List<PatientDimension> patients) {

        assert conceptDims.size() >= 7
        assert patients.size() >= 3

        SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss')
        List<Date> observationStartDates = [
                sdf.parse('2016-10-26 10:00:00'),
                sdf.parse('2016-12-01 10:00:00'),
                sdf.parse('2017-03-15 10:00:00'),
                sdf.parse('2017-03-20 08:30:00')
        ]
        List<Date> observationEndDates = [
                sdf.parse('2016-11-26 10:00:00'),
                sdf.parse('2017-03-01 10:00:00'),
                sdf.parse('2017-03-30 10:00:00'),
                sdf.parse('2017-03-23 08:30:00')
        ]
        List<String> locations = [
                'NWH',
                'BWH',
                'NWH',
                'FH'
        ]
        List<String> providers = [
                '@',
                'Provider_1',
                'Provider_2',
                'Provider_3'
        ]

        def facts = ClinicalTestData.createTabularFacts(conceptDims, patients)

        def allDimsStudy = StudyTestData.createStudy "study", ["patient", "concept", "study", "visit", "trial visit",
                                                               "start time", "end time", "location", "provider"]

        def longitudinalStudy = StudyTestData.createStudy "longitudinal study", ["patient", "concept", "trial visit", "study"]
        def longitudinalClinicalFacts = ClinicalTestData.createLongitudinalFacts(conceptDims[4..5], patients, longitudinalStudy, observationStartDates, observationEndDates,
                locations, providers)

        def sampleStudy = StudyTestData.createStudy "sample study", ["patient", "concept", "study"] // todo: "sample", "testmodifier"
        def sampleClinicalFacts = ClinicalTestData.createSampleFacts(conceptDims[5], patients, sampleStudy, observationStartDates, observationEndDates,
                locations, providers)

        def visits = createTestVisit(3, patients[2], sdf.parse('2016-10-17 10:00:00'), sdf.parse('2016-10-27 10:00:00'))

        def ehrStudy = StudyTestData.createStudy "ehr study", ["patient", "concept", "study", "visit"] // todo: "visit"
        def ehrClinicalFacts = ClinicalTestData.createEhrFacts(conceptDims[6], visits, ehrStudy, observationStartDates, observationEndDates,
                locations, providers)

        new ClinicalTestData(
                patients: patients,
                facts: facts,
                visits: visits,
                longitudinalStudy: longitudinalStudy,
                longitudinalClinicalFacts: longitudinalClinicalFacts,
                sampleStudy: sampleStudy,
                sampleClinicalFacts: sampleClinicalFacts,
                ehrStudy: ehrStudy,
                ehrClinicalFacts: ehrClinicalFacts,
                study:allDimsStudy
        )
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
    static List<ObservationFact> createDiagonalFacts(int count, List<I2b2> concepts, List<PatientDimension> patients) {

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
            int count, List<I2b2> concepts /* terminal */, List<PatientDimension> patients) {

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

    static List<ObservationFact> createCategoricalFacts(Map<PatientDimension, I2b2> values) {
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
                                                 long instanceNum = DUMMY_INSTANCE_ID,
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

    static ObservationFact addModifiersToObservationFact(ObservationFact basicObservationFact,
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

    static List<ObservationFact> extendObservationFactList(List<ObservationFact> basicFactList,
                                                     List<Date> startDates, List<Date> endDates,
                                                     List<String> locations, List<String> providers) {

        def size = basicFactList.size()
        def extendedList = []
        def List<ObservationFact> factList = new ArrayList<ObservationFact>()
        for (int i = 0; i < 3*size; i++){
            int elemNum = i.mod(3)
            def newFact = new ObservationFact()
            InvokerHelper.setProperties(newFact, basicFactList[elemNum].properties)
            factList << newFact
        }
        extendedList += basicFactList
        //different start time and end time
        List<ObservationFact> extendedFactList_1 = new ArrayList<ObservationFact>()
        extendedFactList_1.addAll(factList[0..(size-1)])
        for (int i = 0; i < size; i++) {
            extendedFactList_1[i].setStartDate(startDates[i])
            extendedFactList_1[i].setEndDate(endDates[i])
            extendedFactList_1[i].setLocationCd(locations[i])
        }
        extendedList += extendedFactList_1

        //same start time and location, different end time and providers
        List<ObservationFact> extendedFactList_2 = new ArrayList<ObservationFact>()
        extendedFactList_2.addAll(factList[size..(2*size -1)])
        for (int i = 0; i < size; i++) {
            extendedFactList_2[i].setStartDate(startDates[0])
            extendedFactList_2[i].setProviderId(providers[i])
            extendedFactList_2[i].setEndDate(endDates[i])
        }
        extendedList += extendedFactList_2

        //same end time, providers and location, different start time
        List<ObservationFact> extendedFactList_3 = new ArrayList<ObservationFact>()
        extendedFactList_3.addAll(factList[2*size..(3*size -1)])
        for (int i = 0; i < size; i++) {
            extendedFactList_3[i].setStartDate(startDates[i])
            extendedFactList_3[i].setEndDate(endDates[3])
            extendedFactList_3[i].setProviderId(providers[2])
            extendedFactList_3[i].setLocationCd(locations[2])
        }
        extendedList += extendedFactList_3
        extendedList
    }

    static List<ObservationFact> createTabularFacts(List<ConceptDimension> concepts, List<PatientDimension> patients) {
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

    static List<ObservationFact> createLongitudinalFacts(List<ConceptDimension> concept, List<PatientDimension> patients,
                                                         Study study, List<Date> startDates, List<Date> endDates,
                                                         List<String> locations, List<String> providers){

        def factList = []
        factList << createObservationFact(concept[0].conceptCode, patients[0], DUMMY_ENCOUNTER_ID, 'Homo sapiens', 1, createTrialVisit('day', 2, 'label_1', study))
        factList << createObservationFact(concept[1].conceptCode, patients[1], DUMMY_ENCOUNTER_ID, 'not specified', 1, createTrialVisit('day', 4, 'label_2', study))
        factList << createObservationFact(concept[0].conceptCode, patients[2], DUMMY_ENCOUNTER_ID, 45.0, 1, createTrialVisit('week', 2, 'label_3', study))

        extendObservationFactList(factList, startDates, endDates, locations, providers)
    }

    static List<ObservationFact> createSampleFacts(ConceptDimension concept, List<PatientDimension> patients,
                                                   Study study, List<Date> startDates, List<Date> endDates,
                                                   List<String> locations, List<String> providers){

        def fact = createObservationFact(concept.conceptCode, patients[2], DUMMY_ENCOUNTER_ID, '', 1, createTrialVisit('days', 2, 'label_1', study))
        String modifierCd = 'TEST:TISSUETYPE'

        def factList = []
        factList << fact
        factList << addModifiersToObservationFact(fact, 1, modifierCd, 'CONNECTIVE TISSUE')
        factList << addModifiersToObservationFact(fact, 2, modifierCd, 'MUSCLE TISSUE')

        extendObservationFactList(factList, startDates, endDates, locations, providers)
    }

    static List<ObservationFact> createEhrFacts(ConceptDimension concept, List<VisitDimension> visits,
                                                Study study, List<Date> startDates, List<Date> endDates,
                                                List<String> locations, List<String> providers){
        def ehrFacts = []
        for (int i = 0; i < visits.size(); i++) {
            ehrFacts << createObservationFact(concept.conceptCode, visits[i].patient, visits[i].encounterNum, -45.42,
                    DUMMY_INSTANCE_ID, createTrialVisit('default', 0, null, study))
        }

        extendObservationFactList(ehrFacts, startDates, endDates, locations, providers)
    }

    static TrialVisit createTrialVisit(String relTimeUnit, int relTime, String relTimeLabel, Study study) {
        def tv = new TrialVisit(
                relTimeUnit: relTimeUnit,
                relTime: relTime,
                relTimeLabel: relTimeLabel,
        )
        study.addToTrialVisits(tv)
        tv
    }

    static TrialVisit createDefaultTrialVisit(String relTimeUnit, int relTime) {
        createTrialVisit(relTimeUnit, relTime, null, StudyTestData.createDefaultTabularStudy())
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
