package org.transmartproject.db.dataquery2

import com.google.common.collect.HashMultiset
import com.google.common.collect.Lists
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.TestData
import org.transmartproject.db.TransmartSpecification
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.dataquery2.query.StudyConstraint
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.metadata.DimensionDescription
import static org.hamcrest.Matchers.*
import spock.lang.Ignore

@Integration
@Rollback
class HypercubeIntegrationSpec extends TransmartSpecification {

    TestData testData
    ClinicalTestData clinicalData
    Map<String, Dimension> dims

    @Autowired
    MultidimensionalDataResourceService queryResource
    
    void setupData() {
        testData = TestData.createHypercubeDefault()
        clinicalData = testData.clinicalData
        testData.saveAll()
        dims = DimensionDescription.dimensionsMap
    }

    static private study(String name) {
        new StudyConstraint(studyId: name)
    }

    void 'test_basic_longitudinal_retrieval'() {
        setupData()

        def hypercube = queryResource.doQuery('clinical', constraint: study(clinicalData.longitudinalStudy.studyId))
        def resultObs = Lists.newArrayList(hypercube).sort()

        def result = resultObs*.value as HashMultiset
        hypercube.loadDimensions()
        def concepts = hypercube.dimensionElements(dims.concept) as Set
        def patients = hypercube.dimensionElements(dims.patient) as Set
        def trialVisits = hypercube.dimensionElements(dims.'trial visit') as Set

        def expected = clinicalData.longitudinalClinicalFacts*.value as HashMultiset
        def expectedConcepts = testData.conceptData.conceptDimensions.findAll {
            it.conceptCode in clinicalData.longitudinalClinicalFacts*.conceptCode
        } as Set
        def expectedPatients = clinicalData.longitudinalClinicalFacts*.patient as Set
        def expectedVisits = clinicalData.longitudinalClinicalFacts*.trialVisit as Set

        expect:

        hypercube.dimensionElements.size() == clinicalData.longitudinalStudy.dimensions.size()
        result == expected

        concepts.size() == expectedConcepts.size()
        concepts == expectedConcepts

        patients.size() == expectedPatients.size()
        patients == expectedPatients

        trialVisits.size() == expectedVisits.size()
        trialVisits == expectedVisits

        for (int i = 0; i < resultObs.size(); i++) {
            resultObs[i].getAt(dims.concept).conceptCode == clinicalData.multidimsClinicalFacts[i].conceptCode
            resultObs[i].getAt(dims.patient).id == clinicalData.multidimsClinicalFacts[i].patient.id
            def iTrialVisit = resultObs[i].getAt(dims.'trial visit')
            iTrialVisit.id == clinicalData.multidimsClinicalFacts[i].trialVisit.id
            iTrialVisit.relTime == clinicalData.multidimsClinicalFacts[i].trialVisit.relTime
            iTrialVisit.relTimeLabel == clinicalData.multidimsClinicalFacts[i].trialVisit.relTimeLabel
            iTrialVisit.relTimeUnit == clinicalData.multidimsClinicalFacts[i].trialVisit.relTimeUnit
        }
    }

    @Ignore
    void 'test_basic_sample_retrieval'() {
        setupData()

        def hypercube = queryResource.doQuery('clinical', constraints: [study(clinicalData.sampleStudy.studyId)])
        def resultObs = Lists.newArrayList(hypercube).sort()

        def result = resultObs*.value as HashMultiset
        hypercube.loadDimensions()
        def concepts = hypercube.dimensionElements(dims.concept) as Set
        def patients = hypercube.dimensionElements(dims.patient) as Set

        def expected = clinicalData.sampleClinicalFacts*.value as HashMultiset
        def expectedConcepts = testData.conceptData.conceptDimensions.findAll {
            it.conceptCode in clinicalData.sampleClinicalFacts*.conceptCode
        } as Set
        def expectedPatients = clinicalData.sampleClinicalFacts*.patient as Set

        expect:
        // FIXME Modifiers not supported yet
//        hypercube.dimensionElements.size() == clinicalData.sampleStudy.dimensions.size()
//        result == expected

        concepts.size() == expectedConcepts.size()
        concepts == expectedConcepts

        patients.size() == expectedPatients.size()
        patients == expectedPatients

        for (int i = 0; i < resultObs.size(); i++) {
            resultObs[i].getAt(dims.concept).conceptCode == clinicalData.multidimsClinicalFacts[i].conceptCode
            resultObs[i].getAt(dims.patient).id == clinicalData.multidimsClinicalFacts[i].patient.id
        }
    }

    void 'test_basic_ehr_retrieval'() {
        setupData()

        def hypercube = queryResource.doQuery('clinical', constraint: study(clinicalData.ehrStudy.studyId))
        def resultObs = Lists.newArrayList(hypercube).sort()

        def result = resultObs*.value as HashMultiset
        hypercube.loadDimensions()
        def concepts = hypercube.dimensionElements(dims.concept) as Set
        def patients = hypercube.dimensionElements(dims.patient) as Set
        def visits = hypercube.dimensionElements(dims.visit) as Set

        def expected = clinicalData.ehrClinicalFacts*.value as HashMultiset
        def expectedConcepts = testData.conceptData.conceptDimensions.findAll {
            it.conceptCode in clinicalData.ehrClinicalFacts*.conceptCode
        } as Set
        def expectedPatients = clinicalData.ehrClinicalFacts*.patient as Set
        def expectedVisits = clinicalData.ehrClinicalFacts*.visit as Set

        expect:
        hypercube.dimensionElements.size() == clinicalData.ehrStudy.dimensions.size()
        result == expected

        concepts.size() == expectedConcepts.size()
        concepts == expectedConcepts

        patients.size() == expectedPatients.size()
        patients == expectedPatients

        visits.size() == expectedVisits.size()
        visits == expectedVisits

        for (int i = 0; i < resultObs.size(); i++) {
            resultObs[i].getAt(dims.concept).conceptCode == clinicalData.multidimsClinicalFacts[i].conceptCode
            resultObs[i].getAt(dims.patient).id == clinicalData.multidimsClinicalFacts[i].patient.id
        }
    }

    void 'test_all_dimensions_data_retrieval'() {
        setupData()

        def hypercube = queryResource.doQuery('clinical', constraint: study(clinicalData.multidimsStudy.studyId))
        def resultObs = Lists.newArrayList(hypercube).sort()

        def result = resultObs*.value as HashMultiset
        hypercube.loadDimensions()

        //packable dimensions
        def concepts = hypercube.dimensionElements(dims.concept) as Set
        def patients = hypercube.dimensionElements(dims.patient) as Set
        def trialVisits = hypercube.dimensionElements(dims.'trial visit') as Set
        def visit = hypercube.dimensionElements(dims.'visit') as Set
        def providers = hypercube.dimensionElements(dims.provider) as Set

        // not packable dimensions
        def startTime = resultObs*.getAt(dims.'start time') as Set
        def endTime = resultObs*.getAt(dims.'end time') as Set
        def locations = resultObs*.getAt(dims.location) as Set

        def expected = clinicalData.multidimsClinicalFacts*.value as HashMultiset
        def expectedConcepts = testData.conceptData.conceptDimensions.findAll {
            it.conceptCode in clinicalData.multidimsClinicalFacts*.conceptCode
        } as Set
        def expectedPatients = clinicalData.multidimsClinicalFacts*.patient as Set
        def expectedVisits = clinicalData.multidimsClinicalFacts*.visit.findAll() as Set
        def expectedTrialVisits = clinicalData.multidimsClinicalFacts*.trialVisit as Set
        def expectedStartTime = clinicalData.multidimsClinicalFacts*.startDate as Set
        def expectedEndTime = clinicalData.multidimsClinicalFacts*.endDate as Set
        def expectedLocations = clinicalData.multidimsClinicalFacts*.locationCd as Set
        def expectedProviders = clinicalData.multidimsClinicalFacts*.providerId.findAll{it != '@'} as Set

        expect:

        hypercube.dimensionElements.size() == clinicalData.multidimsStudy.dimensions.size()
        result == expected

        concepts.size() == expectedConcepts.size()
        concepts == expectedConcepts

        patients.size() == expectedPatients.size()
        patients == expectedPatients

        trialVisits.size() == expectedTrialVisits.size()
        trialVisits == expectedTrialVisits

        visit.size() == expectedVisits.size()
        visit == expectedVisits

        startTime.size() == expectedStartTime.size()
        startTime == expectedStartTime

        endTime.size() == expectedEndTime.size()
        endTime == expectedEndTime

        locations.size() == expectedLocations.size()
        locations == expectedLocations

        providers.size() == expectedProviders.size()
        providers == expectedProviders

        for (int i = 0; i < resultObs.size(); i++) {
            resultObs[i].getAt(dims.concept).conceptCode == clinicalData.multidimsClinicalFacts[i].conceptCode
            resultObs[i].getAt(dims.patient).id == clinicalData.multidimsClinicalFacts[i].patient.id
            def iTrialVisit = resultObs[i].getAt(dims.'trial visit')
            iTrialVisit.id == clinicalData.multidimsClinicalFacts[i].trialVisit.id
            iTrialVisit.relTime == clinicalData.multidimsClinicalFacts[i].trialVisit.relTime
            iTrialVisit.relTimeLabel == clinicalData.multidimsClinicalFacts[i].trialVisit.relTimeLabel
            iTrialVisit.relTimeUnit == clinicalData.multidimsClinicalFacts[i].trialVisit.relTimeUnit
            resultObs[i].getAt(dims.provider) == clinicalData.multidimsClinicalFacts[i].providerId
            resultObs[i].getAt(dims.'start time') == clinicalData.multidimsClinicalFacts[i].startDate
            resultObs[i].getAt(dims.'end time') == clinicalData.multidimsClinicalFacts[i].endDate
            resultObs[i].getAt(dims.location) == clinicalData.multidimsClinicalFacts[i].locationCd
        }
    }
}
