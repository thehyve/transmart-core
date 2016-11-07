package org.transmartproject.db.dataquery2

import com.google.common.collect.HashMultiset
import com.google.common.collect.Lists
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.TestData
import org.transmartproject.db.TransmartSpecification
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.metadata.DimensionDescription
import static org.hamcrest.Matchers.*
import spock.lang.Ignore

@Integration
@Rollback
class HypercubeIntegrationSpec extends TransmartSpecification {

    TestData testData
    ClinicalTestData clinicalData
    Map<String,Dimension> dims

    @Autowired
    MultidimensionalDataResourceService queryResource

    void setupData() {
        testData = TestData.createHypercubeDefault()
        clinicalData = testData.clinicalData
        testData.saveAll()
        dims = DimensionDescription.dimensionsMap
    }


    void 'test_basic_longitudinal_retrieval'() {
        setupData()

        def hypercube = queryResource.doQuery(constraints: [study: [clinicalData.longitudinalStudy.studyId]])
        def resultObs = Lists.newArrayList(hypercube)
        def result = resultObs*.value as HashMultiset

        // TODO: hypercubeFacts is bogus, this doesn't test the hypercube code, only if the right query was made
        def hypercubeFacts = hypercube.results.resultSet.result.rows.collect{it[3..it.size()-1]}
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
        def expectedFacts = clinicalData.longitudinalClinicalFacts.collect {
            [it.patient.id, "'"+it.conceptCode+"'", it.trialVisit.id,
             clinicalData.longitudinalStudy.id]
        }

        expect:

        hypercube.dimensionElements.size() ==  clinicalData.longitudinalStudy.dimensions.size()
        result == expected

        concepts.size() == expectedConcepts.size()
        concepts == expectedConcepts

        patients.size() == expectedPatients.size()
        patients == expectedPatients

        trialVisits.size() == expectedVisits.size()
        trialVisits == expectedVisits

        //FIXME better way to compare
        hypercubeFacts.sort().toString() == expectedFacts.sort().toString()
    }

    @Ignore
    void 'test_basic_sample_retrieval'() {
        setupData()

        def hypercube = queryResource.doQuery(constraints: [study: [clinicalData.sampleStudy.studyId]])
        def resultObs = Lists.newArrayList(hypercube)
        def result = resultObs*.value as HashMultiset
        def hypercubeFacts = hypercube.results.resultSet.result.rows.collect{it[3..it.size()-1]}
        hypercube.loadDimensions()
        def concepts = hypercube.dimensionElements(dims.concept) as Set
        def patients = hypercube.dimensionElements(dims.patient) as Set

        def expected = clinicalData.sampleClinicalFacts*.value as HashMultiset
        def expectedConcepts = testData.conceptData.conceptDimensions.findAll {
            it.conceptCode in clinicalData.sampleClinicalFacts*.conceptCode
        } as Set
        def expectedPatients = clinicalData.sampleClinicalFacts*.patient as Set
        def expectedFacts = clinicalData.sampleClinicalFacts.collect {
            [it.patient.id, "'"+it.conceptCode+"'", clinicalData.longitudinalStudy.id]
        }

        expect:
        // FIXME Modifiers not supported yet
//        hypercube.dimensionElements.size() == clinicalData.sampleStudy.dimensions.size()
//        result == expected

        concepts.size() == expectedConcepts.size()
        concepts == expectedConcepts

        patients.size() == expectedPatients.size()
        patients == expectedPatients

        //FIXME better way to compare
        hypercubeFacts.sort().toString() == expectedFacts.sort().toString()
    }

    void 'test_basic_ehr_retrieval'() {
        setupData()

        def hypercube = queryResource.doQuery(constraints: [study: [clinicalData.ehrStudy.studyId]])
        def resultObs = Lists.newArrayList(hypercube)
        def result = resultObs*.value as HashMultiset
        def hypercubeFacts = hypercube.results.resultSet.result.rows.collect{it[3..it.size()-1]}
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
        def expectedFacts = clinicalData.ehrClinicalFacts.collect {
            [it.patient.id, "'"+it.conceptCode+"'", clinicalData.ehrStudy.id,
            it.encounterNum]
        }

        expect:
        hypercube.dimensionElements.size() == clinicalData.ehrStudy.dimensions.size()
        result == expected

        concepts.size() == expectedConcepts.size()
        concepts == expectedConcepts

        patients.size() == expectedPatients.size()
        patients == expectedPatients

        visits.size() == expectedVisits.size()
        visits == expectedVisits

        //FIXME better way to compare
        hypercubeFacts.sort().toString() == expectedFacts.sort().toString()
    }

    @Ignore
    void 'test_all_dimensions_data_retrieval'() {
        setupData()

        def hypercube = queryResource.doQuery(constraints: [study: [clinicalData.multidimsStudy.studyId]])
        def resultObs = Lists.newArrayList(hypercube)
        def result = resultObs*.value as HashMultiset
        def hypercubeFacts = hypercube.results.resultSet.result.rows.collect{it[3..it.size()-1]}
        hypercube.loadDimensions()

        //FIXME: SPARSE dimensions not working
        def concepts = hypercube.dimensionElements(dims.concept) as Set
        def patients = hypercube.dimensionElements(dims.patient) as Set
        def trialVisits = hypercube.dimensionElements(dims.'trial visit') as Set
        def visit = hypercube.dimensionElements(dims.'visit') as Set
        def startTime = hypercube.dimensionElements(dims.'start time') as Set
        def endTime = hypercube.dimensionElements(dims.'end time') as Set
        def locations = hypercube.dimensionElements(dims.'location') as Set
        def providers = hypercube.dimensionElements(dims.'provider') as Set

        def expected = clinicalData.multidimsClinicalFacts*.value as HashMultiset
        def expectedConcepts = testData.conceptData.conceptDimensions.findAll {
            it.conceptCode in clinicalData.multidimsClinicalFacts*.conceptCode
        } as Set
        def expectedPatients = clinicalData.multidimsClinicalFacts*.patient as Set
        def expectedVisits = clinicalData.multidimsClinicalFacts*.visit as Set
        def expectedTrialVisits = clinicalData.longitudinalClinicalFacts*.trialVisit as Set
        def expectedStartTime = clinicalData.multidimsClinicalFacts*.startDate as Set
        def expectedEndTime = clinicalData.multidimsClinicalFacts*.endDate as Set
        def expectedLocations = clinicalData.multidimsClinicalFacts*.locationCd as Set
        def expectedProviders = clinicalData.multidimsClinicalFacts*.providerId as Set
        def expectedFacts = clinicalData.multidimsClinicalFacts.collect {
            [it.patient.id, "'"+it.conceptCode+"'", it.trialVisit.id,
             clinicalData.multidimsStudy.id]
        }

        expect:

        hypercube.dimensionElements.size() ==  clinicalData.multidimsStudy.dimensions.size()
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

        //FIXME better way to compare
        hypercubeFacts.sort().toString() == expectedFacts.sort().toString()
    }
}
