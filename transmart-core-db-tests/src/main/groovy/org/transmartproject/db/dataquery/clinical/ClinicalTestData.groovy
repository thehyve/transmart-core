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
import org.transmartproject.db.i2b2data.*
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.multidimquery.ModifierDimension
import org.transmartproject.db.ontology.AcrossTrialsTestData
import org.transmartproject.db.ontology.I2b2
import org.transmartproject.db.ontology.ModifierDimensionCoreDb
import org.transmartproject.db.querytool.QtQueryMaster

import java.math.RoundingMode
import java.text.SimpleDateFormat

import static com.google.common.collect.Iterators.cycle
import static com.google.common.collect.Iterators.peekingIterator
import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.LEAF
import static org.transmartproject.db.querytool.QueryResultData.createQueryResult
import static org.transmartproject.db.querytool.QueryResultData.getQueryResultFromMaster
import static org.transmartproject.core.multidimquery.hypercube.Dimension.Density.*
import static org.transmartproject.core.multidimquery.hypercube.Dimension.Size.*
import static org.transmartproject.core.multidimquery.hypercube.Dimension.Packable.*

class ClinicalTestData {

    public static final long DUMMY_ENCOUNTER_ID = -1L
    public static final long DUMMY_INSTANCE_ID = 1L

    Study                   defaultStudy
    TrialVisit              defaultTrialVisit
    List<PatientDimension>  patients
    List<VisitDimension>    visits
    List<ObservationFact>   facts
    Study                   longitudinalStudy
    List<ObservationFact>   longitudinalClinicalFacts
    Study                   sampleStudy
    List<ObservationFact>   sampleClinicalFacts
    Study                   ehrStudy
    List<ObservationFact>   ehrClinicalFacts
    Study                   multidimsStudy
    List<ObservationFact>   multidimsClinicalFacts
    List<ModifierDimensionCoreDb> modifierDimensions
    ModifierDimension       doseDimension
    ModifierDimension       tissueTypeDimension

    List<ObservationFact> getAllHypercubeFacts() {
        longitudinalClinicalFacts + sampleClinicalFacts + ehrClinicalFacts + multidimsClinicalFacts
    }
    List<Study> getAllHypercubeStudies() {
        [longitudinalStudy, sampleStudy, ehrStudy, multidimsStudy]
    }

    @Lazy
    QtQueryMaster patientsQueryMaster = createQueryResult 'clinical-patients-set', patients

    QueryResult getQueryResult() {
        getQueryResultFromMaster patientsQueryMaster
    }

    static ClinicalTestData createDefault(List<I2b2> concepts, List<PatientDimension> patients, TrialVisit trialVisit) {
        def facts = createDiagonalFacts(2, concepts, patients, trialVisit)
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

        def visits = createTestVisit(3, patients[2], sdf.parse('2016-10-17 10:00:00'), sdf.parse('2016-10-27 10:00:00')) + createTestVisit(3, patients[1], sdf.parse('2016-11-09 10:30:00'), sdf.parse('2016-12-27 10:00:00'))
        visits*.save()

        def defaultStudy = StudyTestData.createDefaultTabularStudy()
        def trialVisit = new TrialVisit(study: defaultStudy, relTimeUnit: 'week', relTime: 3, relTimeLabel: '3 weeks')

        def facts = createTabularFacts(conceptDims, patients, trialVisit)

        def multidimsStudy = StudyTestData.createStudy "multidimensional study", ["patient", "concept", "study", "visit", "trial visit",
                                                               "start time", "end time", "location", "provider"], true
        def multidimsClinicalFacts = createMultidimensionalFacts(conceptDims[5..6], visits[3..5], multidimsStudy, observationStartDates, observationEndDates,
                locations, providers)

        def longitudinalStudy = StudyTestData.createStudy "longitudinal study", ["patient", "concept", "trial visit", "study"]
        def longitudinalClinicalFacts = createLongitudinalFacts(conceptDims[4..5], patients, longitudinalStudy, observationStartDates, observationEndDates,
                locations, providers)

        def doseDimension = new DimensionDescription(name: "dose", modifierCode: "TEST:DOSE", valueType: ObservationFact.TYPE_NUMBER,
                size: LARGE, density: SPARSE).save()
        def tissueTypeDimension = new DimensionDescription(name: "tissueType", modifierCode: "TEST:TISSUETYPE", valueType: ObservationFact.TYPE_TEXT,
                packable: PACKABLE).save()

        def sampleStudy = StudyTestData.createStudy "sample study", ["patient", "concept", "study",
                "dose", "tissueType"] // todo: "sample"
        def sampleClinicalFacts = createSampleFacts(conceptDims[5], patients, sampleStudy, observationStartDates, observationEndDates,
                locations, providers)

        def ehrStudy = StudyTestData.createStudy "ehr study", ["patient", "concept", "study", "visit"]
        def ehrClinicalFacts = createEhrFacts(conceptDims[6], visits[0..2], ehrStudy, observationStartDates, observationEndDates,
                locations, providers)

        def modifierDimensions = AcrossTrialsTestData.createModifier(path: '\\Public Studies\\Medications\\Doses\\',
                code:'TEST:DOSE', nodeType: 'F')

        new ClinicalTestData(
                defaultStudy: defaultStudy,
                defaultTrialVisit: trialVisit,
                patients: patients,
                facts: facts,
                visits: visits,
                longitudinalStudy: longitudinalStudy,
                longitudinalClinicalFacts: longitudinalClinicalFacts,
                sampleStudy: sampleStudy,
                sampleClinicalFacts: sampleClinicalFacts,
                doseDimension: doseDimension.dimension as ModifierDimension,
                tissueTypeDimension: tissueTypeDimension.dimension as ModifierDimension,
                ehrStudy: ehrStudy,
                ehrClinicalFacts: ehrClinicalFacts,
                multidimsStudy:multidimsStudy,
                multidimsClinicalFacts: multidimsClinicalFacts,
                modifierDimensions: [modifierDimensions.left]
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
    static List<ObservationFact> createDiagonalFacts(int count, List<I2b2> concepts, List<PatientDimension> patients, TrialVisit trialVisit) {

        assert patients.size() >= count

        def leafConceptsCodes = concepts.findAll {
            LEAF in it.visualAttributes
        } collect { it.code }

        assert leafConceptsCodes.size() >= count

        def facts = []
        for (int i = 0; i < count; i++) {
            facts << createObservationFact(leafConceptsCodes[i], patients[i],
                    DUMMY_ENCOUNTER_ID, Math.pow(10, i + 1), trialVisit)
        }
        facts
    }

    static List<ObservationFact> createDiagonalCategoricalFacts(
            int count, List<I2b2> concepts /* terminal */, List<PatientDimension> patients, TrialVisit trialVisit) {

        assert patients.size() >= count
        assert concepts.size() > 0

        def map = [:]
        for (int i = 0; i < count; i++) {
            int j = i % concepts.size()
            assert LEAF in concepts[j].visualAttributes //leaf
            assert !concepts[j].metadata?.okToUseValues  //non-numeric

            map[patients[i]] = concepts[j]
        }

        createCategoricalFacts map, trialVisit
    }

    static List<ObservationFact> createCategoricalFacts(Map<PatientDimension, I2b2> values, TrialVisit trialVisit) {
        values.collect { patient, i2b2 ->
            createObservationFact(
                    i2b2.code, patient, DUMMY_ENCOUNTER_ID, i2b2.name, trialVisit)
        }
    }

    static ObservationFact createObservationFact(ConceptDimension concept,
                                                 PatientDimension patient,
                                                 Long encounterId,
                                                 Object value,
                                                 TrialVisit trialVisit) {

        createObservationFact(concept.conceptCode, patient, encounterId, value, DUMMY_INSTANCE_ID, trialVisit)
    }

    static ObservationFact createObservationFact(Map args) {
        [
            modifierCd: '@',
            providerId: 'fakeProviderId',
            encounterNum: DUMMY_ENCOUNTER_ID,
            startDate: new Date(),
            instanceNum: DUMMY_INSTANCE_ID,
        ].each { prop, val -> args.putIfAbsent(prop, val) }

        def value = args.remove('value')
        def tv = args.remove('trialVisit') as TrialVisit

        def of = new ObservationFact(args)

        tv.addToObservationFacts(of)

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

    static ObservationFact createObservationFact(String conceptCode,
                                                 PatientDimension patient,
                                                 long encounterId,
                                                 Object value,
                                                 long instanceNum = DUMMY_INSTANCE_ID,
                                                 TrialVisit trialVisit){

        def of = new ObservationFact(
                encounterNum: encounterId,
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
                                                         String modifierCd,
                                                         Object value) {

        def extendedFact = new ObservationFact()
        InvokerHelper.setProperties(extendedFact, basicObservationFact.properties)

        extendedFact.modifierCd = modifierCd

        if (value instanceof Number) {
            extendedFact.valueType = ObservationFact.TYPE_NUMBER
            extendedFact.textValue = 'E' //equal to
            extendedFact.numberValue = value as BigDecimal
        } else if (value != null) {
            extendedFact.valueType = ObservationFact.TYPE_TEXT
            extendedFact.textValue = value as String
            extendedFact.numberValue = null
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

    static List<ObservationFact> createTabularFacts(List<ConceptDimension> concepts, List<PatientDimension> patients, TrialVisit trialVisit) {
        long encounterNum = -200
        def list1 = concepts[0..1].collect { ConceptDimension concept ->
            patients.collect { PatientDimension patient ->
                createObservationFact(concept.conceptCode, patient, --encounterNum,
                        "value for $concept.conceptCode/$patient.id", DUMMY_INSTANCE_ID, trialVisit)
            }
        }.inject([], { accum, factList -> accum + factList })

        list1 + [
                // missing fact for patients[0]
                createObservationFact(concepts[2].conceptCode, patients[1], --encounterNum, '', DUMMY_INSTANCE_ID, trialVisit), //empty value
                createObservationFact(concepts[2].conceptCode, patients[2], --encounterNum, -45.42, DUMMY_INSTANCE_ID, trialVisit) //numeric value
        ]
    }

    static List<ObservationFact> createLongitudinalFacts(List<ConceptDimension> concept, List<PatientDimension> patients,
                                                         Study study, List<Date> startDates, List<Date> endDates,
                                                         List<String> locations, List<String> providers){

        def factList = []

        def v = 45.0
        def instancenum = 1
        def trialVisits = [createTrialVisit('day', 0, 'baseline', study),
                           createTrialVisit('day', 10, 'after 10 days', study),
                           createTrialVisit('day', 35, 'after 5 weeks', study)]
        patients.each { p ->
            trialVisits.each { tv ->
                factList << createObservationFact(concept[0].conceptCode, p, DUMMY_ENCOUNTER_ID, v++, instancenum++, tv)
                factList << createObservationFact(concept[1].conceptCode, p, DUMMY_ENCOUNTER_ID, "Homo Sapiens ${v++}", instancenum++, tv)
            }
        }

        //extendObservationFactList(factList, startDates, endDates, locations, providers)
        factList
    }

    static List<ObservationFact> createSampleFacts(ConceptDimension concept, List<PatientDimension> patients,
                                                   Study study, List<Date> startDates, List<Date> endDates,
                                                   List<String> locations, List<String> providers){

        def trialVisit = createTrialVisit(null, null, 'baseline', study)
        def factList = []
        // Sort by ascending patient id (-103, -102, -101), so that the sort that the hypercube does due to the
        // modifiers doesn't change the order
        patients.sort({it.id}).each { patient ->
            def fact1 = createObservationFact(concept.conceptCode, patient, DUMMY_ENCOUNTER_ID, 'first sample', 1, trialVisit)
            def fact2 = createObservationFact(concept.conceptCode, patient, DUMMY_ENCOUNTER_ID, 'second sample', 2, trialVisit)
            String tissueCode = 'TEST:TISSUETYPE'
            String doseCode = 'TEST:DOSE'

            factList << fact1
            factList << fact2
            factList << addModifiersToObservationFact(fact1, tissueCode, 'CONNECTIVE TISSUE')
            factList << addModifiersToObservationFact(fact2, tissueCode, 'MUSCLE TISSUE')
            factList << addModifiersToObservationFact(fact1, doseCode, 325)
            factList << addModifiersToObservationFact(fact2, doseCode, 630)
        }

        factList
        //extendObservationFactList(factList, startDates, endDates, locations, providers)
    }

    static List<ObservationFact> createEhrFacts(ConceptDimension concept, List<VisitDimension> visits,
                                                Study study, List<Date> startDates, List<Date> endDates,
                                                List<String> locations, List<String> providers){
        def ehrFacts = []
        for (int i = 0; i < visits.size(); i++) {
            ehrFacts << createObservationFact(concept.conceptCode, visits[i].patient, visits[i].id, -45.42,
                    DUMMY_INSTANCE_ID, createTrialVisit('default', 0, null, study))
        }
        extendObservationFactList(ehrFacts, startDates, endDates, locations, providers)
    }


    static List<ObservationFact> createMultidimensionalFacts (List<ConceptDimension> concept, List<VisitDimension> visits,
                                                         Study study, List<Date> startDates, List<Date> endDates,
                                                         List<String> locations, List<String> providers){

        // These trialVisits don't really make sense, since different trial visits share the same visit, but it works
        // fine for testing
        def trialVisits = [createTrialVisit('day', 0, 'baseline', study),
                           createTrialVisit('day', 10, 'after 10 days', study),
                           createTrialVisit('day', 35, 'after 5 weeks', study)]
        def istartDates = peekingIterator(cycle(startDates))
        def iendDates = peekingIterator(cycle(endDates))
        def ilocations = peekingIterator(cycle(locations))
        def iproviders = peekingIterator(cycle(providers))
        def factList = []
        def val = 42.1
        def instanceNum = 1
        visits.each { visit -> trialVisits.each { tv ->
            factList << createObservationFact(
                    conceptCode: concept[0].conceptCode,
                    patient: visit.patient,
                    encounterNum: visit.id,
                    value: val++,
                    instanceNum: instanceNum++,
                    trialVisit: tv,
                    startDate: istartDates.peek(),
                    endDate: iendDates.peek(),
                    locationCd: ilocations.peek(),
                    providerId: iproviders.peek(),
                )
            factList << createObservationFact(
                    conceptCode: concept[1].conceptCode,
                    patient: visit.patient,
                    encounterNum: visit.id,
                    value: "measurement ${val++}",
                    instanceNum: instanceNum++,
                    trialVisit: tv,
                    startDate: istartDates.next(),
                    endDate: iendDates.next(),
                    locationCd: ilocations.next(),
                    providerId: iproviders.next(),
            )
        }}

        factList
    }

    static TrialVisit createTrialVisit(String relTimeUnit, Integer relTime, String relTimeLabel, Study study) {
        def tv = new TrialVisit(
                relTimeUnit: relTimeUnit,
                relTime: relTime,
                relTimeLabel: relTimeLabel,
        )
        study.addToTrialVisits(tv)
        tv
    }

    static List<VisitDimension> createTestVisit(int n, PatientDimension patient, Date startDate, Date endDate) {
        (1..n).collect { int i ->
            def visit = new VisitDimension(
                    patient: patient,
                    startDate: startDate + (10*i),
                    endDate: endDate + (10*i),
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
        TestDataHelper.save multidimsClinicalFacts
        TestDataHelper.save modifierDimensions
        TestDataHelper.save allHypercubeStudies.findAll()
    }

}
