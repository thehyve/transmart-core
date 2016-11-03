package org.transmartproject.db.dataquery2

import com.google.common.collect.HashMultiset
import com.google.common.collect.Lists
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.Ignore
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.TestData
import org.transmartproject.db.TransmartSpecification
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.TrialVisit
import org.transmartproject.db.metadata.DimensionDescription
import static org.hamcrest.Matchers.*

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

        def hypercube = queryResource.doQuery(constraints: [study: [clinicalData.longitudinalStudy.name]])
        def resultObs = Lists.newArrayList(hypercube)
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

        hypercube.dimensionElements.size() ==  clinicalData.longitudinalStudy.dimensions.size()
        result == expected

        concepts.size() == expectedConcepts.size()
        concepts == expectedConcepts

        patients.size() == expectedPatients.size()
        patients == expectedPatients

        trialVisits.size() == expectedVisits.size()
        trialVisits == expectedVisits
    }


    void 'test_basic_sample_retrieval'() {
        setupData()

        def hypercube = queryResource.doQuery(constraints: [study: [clinicalData.sampleStudy.name]])
        def resultObs = Lists.newArrayList(hypercube)
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
    }

    void 'test_basic_ehr_retrieval'() {
        setupData()

        def hypercube = queryResource.doQuery(constraints: [study: [clinicalData.ehrStudy.name]])
        def resultObs = Lists.newArrayList(hypercube)
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
        hypercube.dimensionElements.size() ==  clinicalData.longitudinalStudy.dimensions.size()
        result == expected

        concepts.size() == expectedConcepts.size()
        concepts == expectedConcepts

        patients.size() == expectedPatients.size()
        patients ==expectedPatients

        visits.size() == expectedVisits.size()
        visits == expectedVisits
    }

    @Ignore
    void 'test_all_dimensions_data_retrieval'() {
        setupData()

        expect:
        1==1
    }
}
