package org.transmartproject.db.multidimquery

import com.google.common.collect.HashMultiset
import com.google.common.collect.Lists
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.DataRetrievalParameters
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.db.TestData
import org.transmartproject.db.TransmartSpecification
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.core.multidimquery.query.StudyNameConstraint
import org.transmartproject.db.user.AccessLevelTestData
import org.transmartproject.db.user.User

import static org.transmartproject.db.multidimquery.DimensionImpl.*


@Integration
@Rollback
class HypercubeIntegrationSpec extends TransmartSpecification {

    @Autowired
    SessionFactory sessionFactory

    TestData testData
    ClinicalTestData clinicalData
    AccessLevelTestData accessLevelTestData
    User adminUser


    @Autowired
    MultidimensionalDataResourceService queryResource

    void setupData() {
        TestData.clearAllData()

        testData = TestData.createHypercubeDefault()
        clinicalData = testData.clinicalData

        // Intentionally remove one modifier row
        def lastSamplePatient = clinicalData.sampleClinicalFacts.findAll {
            it.modifierCd == 'TEST:DOSE'
        }*.patient.sort({it.id})[-1]
        clinicalData.sampleClinicalFacts.removeAll {
            it.modifierCd == 'TEST:DOSE' && it.patient.id == lastSamplePatient.id
        }

        testData.saveAll()

        accessLevelTestData = new AccessLevelTestData()
        accessLevelTestData.saveAuthorities()
        adminUser = accessLevelTestData.users[0]

        sessionFactory.currentSession.flush()
    }

    static private study(String name) {
        new StudyNameConstraint(studyId: name)
    }

    static private def pattern = ~"[0-9]+(\\.[0-9]+)?\$"
    static private def getKey = { HypercubeValue v ->
        if(v.value in String) {
            def m = pattern.matcher(v.value)
            m.find()
            return m.group() as BigDecimal
        }
        return v.value as BigDecimal
    }

    static private trAt(val) {
        val == '@' ? null : val
    }

    void 'test_basic_longitudinal_retrieval'() {
        setupData()

        def args = new DataRetrievalParameters(constraint: study(clinicalData.longitudinalStudy.studyId))
        def hypercube = queryResource.retrieveData(args, 'clinical', adminUser)
        def resultObs = Lists.newArrayList(hypercube).sort(getKey)

        def result = resultObs*.value as HashMultiset

        def patients = hypercube.dimensionElements(PATIENT) as Set
        def trialVisits = hypercube.dimensionElements(TRIAL_VISIT) as Set
        def concepts = hypercube.dimensionElements(CONCEPT) as Set

        def expected = clinicalData.longitudinalClinicalFacts*.value as HashMultiset
        def expectedConcepts = testData.conceptData.conceptDimensions.findAll {
            it.conceptCode in clinicalData.longitudinalClinicalFacts*.conceptCode
        } as Set
        def expectedPatients = clinicalData.longitudinalClinicalFacts*.patient as Set
        def expectedVisits = clinicalData.longitudinalClinicalFacts*.trialVisit as Set

        expect:

        hypercube.dimensions.size() == clinicalData.longitudinalStudy.dimensions.size()
        result == expected

        concepts.size() == expectedConcepts.size()
        concepts == expectedConcepts

        patients.size() == expectedPatients.size()
        patients == expectedPatients

        trialVisits.size() == expectedVisits.size()
        trialVisits == expectedVisits

        for (int i = 0; i < resultObs.size(); i++) {
            assert resultObs[i].getAt(CONCEPT).conceptCode == clinicalData.longitudinalClinicalFacts[i].conceptCode
            assert resultObs[i].getAt(PATIENT).id == clinicalData.longitudinalClinicalFacts[i].patient.id
            assert resultObs[i].value == clinicalData.longitudinalClinicalFacts[i].value
            def iTrialVisit = resultObs[i].getAt(TRIAL_VISIT)
            assert iTrialVisit.id == clinicalData.longitudinalClinicalFacts[i].trialVisit.id
            assert iTrialVisit.relTime == clinicalData.longitudinalClinicalFacts[i].trialVisit.relTime
            assert iTrialVisit.relTimeLabel == clinicalData.longitudinalClinicalFacts[i].trialVisit.relTimeLabel
            assert iTrialVisit.relTimeUnit == clinicalData.longitudinalClinicalFacts[i].trialVisit.relTimeUnit
        }
    }

    void 'test_basic_sample_retrieval'() {
        setupData()
        def ttDim = clinicalData.tissueTypeDimension
        def doseDim = clinicalData.doseDimension

        def args = new DataRetrievalParameters(constraint: study(clinicalData.sampleStudy.studyId))
        def hypercube = queryResource.retrieveData(args, 'clinical', adminUser)
        def resultObs = Lists.newArrayList(hypercube) // TODO: make observations sortable to ensure order

        def resultValues = resultObs*.value as HashMultiset

        def concepts = hypercube.dimensionElements(CONCEPT) as Set
        def patients = hypercube.dimensionElements(PATIENT) as Set
        def tissueTypes = hypercube.dimensionElements(ttDim) as Set

        def expected = clinicalData.sampleClinicalFacts.findAll{it.modifierCd == '@'}
        def expectedValues = expected*.value as HashMultiset
        def expectedConcepts = testData.conceptData.conceptDimensions.findAll {
            it.conceptCode in expected*.conceptCode
        } as Set
        def expectedPatients = expected*.patient as Set
        def expectedTissues = clinicalData.sampleClinicalFacts.findAll{it.modifierCd == ttDim.modifierCode}*.textValue as Set
        def expectedDosages = clinicalData.sampleClinicalFacts.findAll{it.modifierCd == doseDim.modifierCode}*.numberValue as Set
        expectedDosages << null // a missing dose

        expect:
        hypercube.dimensions.size() == clinicalData.sampleStudy.dimensions.size()
        resultValues == expectedValues

        concepts.size() == expectedConcepts.size()
        concepts == expectedConcepts

        patients.size() == expectedPatients.size()
        patients == expectedPatients

        tissueTypes.size() == expectedTissues.size()
        tissueTypes == expectedTissues

        expectedDosages == resultObs*.getAt(doseDim) as Set

        // This makes assumptions on the order of observation facts in the test data
        for (int i = 0; i < resultObs.size(); i++) {
            assert resultObs[i][CONCEPT].conceptCode ==
                    clinicalData.sampleClinicalFacts.findAll{it.modifierCd == '@'}[i].conceptCode
            assert resultObs[i][PATIENT].id ==
                    clinicalData.sampleClinicalFacts.findAll{it.modifierCd == '@'}[i].patient.id
            assert resultObs[i][ttDim] ==
                    clinicalData.sampleClinicalFacts.findAll{it.modifierCd == ttDim.modifierCode}[i].textValue
            assert resultObs[i][doseDim] ==
                    clinicalData.sampleClinicalFacts.findAll{it.modifierCd == doseDim.modifierCode}[i]?.numberValue
        }
    }

    void 'test_sample_retrieval_with_partial_dimensions'() {
        setupData()
        def ttDim = clinicalData.tissueTypeDimension
        def doseDim = clinicalData.doseDimension

        def args = new DataRetrievalParameters(
                constraint: study(clinicalData.sampleStudy.studyId),
                dimensions: ['study', 'concept', 'tissueType', 'dose']
        )
        def hypercube = queryResource.retrieveData(args, 'clinical', adminUser)
        def resultObs = Lists.newArrayList(hypercube.iterator()) // TODO: make observations sortable to ensure order

        def resultValues = resultObs*.value as HashMultiset
        def concepts = hypercube.dimensionElements(CONCEPT) as Set
        def tissueTypes = hypercube.dimensionElements(ttDim) as Set

        def expected = clinicalData.sampleClinicalFacts.findAll{it.modifierCd == '@'}
        def expectedValues = expected*.value as HashMultiset
        def expectedConcepts = testData.conceptData.conceptDimensions.findAll {
            it.conceptCode in expected*.conceptCode
        } as Set
        def expectedTissues = clinicalData.sampleClinicalFacts.findAll{it.modifierCd == ttDim.modifierCode}*.textValue as Set
        def expectedDosages = clinicalData.sampleClinicalFacts.findAll{it.modifierCd == doseDim.modifierCode}*.numberValue as Set
        expectedDosages << null // a missing dose

        expect:
        !(PATIENT in hypercube.dimensions)

        !resultObs.empty

        hypercube.dimensions.size() == clinicalData.sampleStudy.dimensions.size() - 1
        resultValues == expectedValues

        concepts.size() == expectedConcepts.size()
        concepts == expectedConcepts

        tissueTypes.size() == expectedTissues.size()
        tissueTypes == expectedTissues

        expectedDosages == resultObs*.getAt(doseDim) as Set

        when:
        hypercube.dimensionElements(PATIENT)

        then:
        thrown(IllegalArgumentException)

    }

    void 'test_basic_ehr_retrieval'() {
        setupData()

        def args = new DataRetrievalParameters(constraint: study(clinicalData.ehrStudy.studyId))
        def hypercube = queryResource.retrieveData(args, 'clinical', adminUser)
        def resultObs = Lists.newArrayList(hypercube) // TODO: make observations sortable to ensure order

        def result = resultObs*.value as HashMultiset

        def patients = hypercube.dimensionElements(PATIENT) as Set
        def visits = hypercube.dimensionElements(VISIT) as Set
        def concepts = hypercube.dimensionElements(CONCEPT) as Set

        def expected = clinicalData.ehrClinicalFacts*.value as HashMultiset
        def expectedConcepts = testData.conceptData.conceptDimensions.findAll {
            it.conceptCode in clinicalData.ehrClinicalFacts*.conceptCode
        } as Set
        def expectedPatients = clinicalData.ehrClinicalFacts*.patient as Set
        def expectedVisits = clinicalData.ehrClinicalFacts*.visit as Set

        expect:
        hypercube.dimensions.size() == clinicalData.ehrStudy.dimensions.size()
        result == expected

        concepts.size() == expectedConcepts.size()
        concepts == expectedConcepts

        patients.size() == expectedPatients.size()
        patients == expectedPatients

        visits.size() == expectedVisits.size()
        visits == expectedVisits

        for (int i = 0; i < resultObs.size(); i++) {
            assert resultObs[i].getAt(CONCEPT).conceptCode == clinicalData.ehrClinicalFacts[i].conceptCode
            assert resultObs[i].getAt(PATIENT).id == clinicalData.ehrClinicalFacts[i].patient.id
        }
    }

    void 'test_all_dimensions_data_retrieval'() {
        setupData()

        when:
        def args = new DataRetrievalParameters(constraint: study(clinicalData.multidimsStudy.studyId))
        def hypercube = queryResource.retrieveData(args, 'clinical', adminUser)
        then:
        hypercube != null
        hypercube.dimensions.size() == clinicalData.multidimsStudy.dimensions.size()

        when:
        def resultObs = Lists.newArrayList(hypercube)
        then:
        matchesAllExpectedIndexedDimensionElements(hypercube)
        matchesAllExpectedInlineDimensionElements(resultObs)
        matchesAllExpectedValues(resultObs)

        when: 'we request dimension elements after the completed scan of data'
        !hypercube.dimensionElements(PATIENT).empty
        then: 'no more scans required to get dimension elements'
        hypercube.completeScanNumber == 1

        when:
        def sortedResultObs = resultObs.sort(getKey)
        then:
        for (int i = 0; i < resultObs.size(); i++) {
            assert sortedResultObs[i].getAt(CONCEPT).conceptCode == clinicalData.multidimsClinicalFacts[i].conceptCode
            assert sortedResultObs[i].getAt(PATIENT).id == clinicalData.multidimsClinicalFacts[i].patient.id
            def iTrialVisit = sortedResultObs[i].getAt(TRIAL_VISIT)
            assert iTrialVisit.id == clinicalData.multidimsClinicalFacts[i].trialVisit.id
            assert iTrialVisit.relTime == clinicalData.multidimsClinicalFacts[i].trialVisit.relTime
            assert iTrialVisit.relTimeLabel == clinicalData.multidimsClinicalFacts[i].trialVisit.relTimeLabel
            assert iTrialVisit.relTimeUnit == clinicalData.multidimsClinicalFacts[i].trialVisit.relTimeUnit
            assert sortedResultObs[i].getAt(PROVIDER) == trAt(clinicalData.multidimsClinicalFacts[i].providerId)
            assert sortedResultObs[i].getAt(START_TIME) == trAt(clinicalData.multidimsClinicalFacts[i].startDate)
            assert sortedResultObs[i].getAt(END_TIME) == trAt(clinicalData.multidimsClinicalFacts[i].endDate)
            assert sortedResultObs[i].getAt(LOCATION) == trAt(clinicalData.multidimsClinicalFacts[i].locationCd)
        }
    }

    void 'test_all_dimensions_preloaded'() {
        setupData()

        when:
        def args = new DataRetrievalParameters(constraint: study(clinicalData.multidimsStudy.studyId))
        def hypercube = queryResource.retrieveData(args, 'clinical', adminUser)
        then:
        hypercube != null
        hypercube.dimensions.size() == clinicalData.multidimsStudy.dimensions.size()

        when: 'get any dimension elements'
        !hypercube.dimensionElements(PATIENT).empty
        then: 'all dimensions elements are pre-populated'
        matchesAllExpectedIndexedDimensionElements(hypercube)
        hypercube.completeScanNumber == 1
        when: 'read all value cells'
        List<HypercubeValue> valueCells = Lists.newArrayList(hypercube)
        then:
        hypercube.completeScanNumber == 2
        matchesAllExpectedInlineDimensionElements(valueCells)
        matchesAllExpectedValues(valueCells)
    }

    void matchesAllExpectedIndexedDimensionElements(Hypercube hypercube) {
        def concepts = hypercube.dimensionElements(CONCEPT) as Set
        def patients = hypercube.dimensionElements(PATIENT) as Set
        def trialVisits = hypercube.dimensionElements(TRIAL_VISIT) as Set
        def visit = hypercube.dimensionElements(VISIT) as Set

        def expectedConcepts = testData.conceptData.conceptDimensions.findAll {
            it.conceptCode in clinicalData.multidimsClinicalFacts*.conceptCode
        } as Set
        def expectedPatients = clinicalData.multidimsClinicalFacts*.patient as Set
        def expectedVisits = clinicalData.multidimsClinicalFacts*.visit.findAll() as Set
        def expectedTrialVisits = clinicalData.multidimsClinicalFacts*.trialVisit as Set

        assert concepts == expectedConcepts
        assert patients == expectedPatients
        assert trialVisits == expectedTrialVisits
        assert visit == expectedVisits
    }

    void matchesAllExpectedInlineDimensionElements(List<HypercubeValue> valueCells) {
        def expectedStartTime = clinicalData.multidimsClinicalFacts*.startDate as Set
        def expectedEndTime = clinicalData.multidimsClinicalFacts*.endDate as Set
        def expectedLocations = clinicalData.multidimsClinicalFacts*.locationCd as Set
        def expectedProviders = clinicalData.multidimsClinicalFacts*.providerId.findAll{it != '@'} as Set

        def startTime = valueCells*.getAt(START_TIME) as Set
        def endTime = valueCells*.getAt(END_TIME) as Set
        def locations = valueCells*.getAt(LOCATION) as Set
        def providers = valueCells*.getAt(PROVIDER) as Set
        providers.remove(null)

        assert startTime == expectedStartTime
        assert endTime == expectedEndTime
        assert locations == expectedLocations
        assert providers == expectedProviders
    }

    void matchesAllExpectedValues(List<HypercubeValue> valueCells) {
        def values = valueCells*.value as HashMultiset
        def expectedValues = clinicalData.multidimsClinicalFacts*.value as HashMultiset

        assert values == expectedValues
    }

}
