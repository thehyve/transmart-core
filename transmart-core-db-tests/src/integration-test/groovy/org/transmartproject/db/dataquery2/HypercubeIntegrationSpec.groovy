package org.transmartproject.db.dataquery2

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.TestData
import org.transmartproject.db.TransmartSpecification
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.dataquery.clinical.ClinicalTestData

@Integration
@Rollback
class HypercubeIntegrationSpec extends TransmartSpecification {

    TestData testData
    ClinicalTestData clinicalData

    @Autowired
    MultidimensionalDataResourceService queryResource

    void setupData() {
        testData = TestData.createHypercubeDefault()
        testData.saveAll()
        clinicalData = testData.clinicalData
    }

    void testTest() {
        setupData()
        def it = method(1,1)

        expect:
        it == 2
    }

    int method(a, b) {
        return a+b
    }

    void testTest2() {
        setupData()
        def it = 3

        expect:
        it == 3
    }
//
//    void 'test basic longitudinal retrieval'() {
//        setupData()
//
//        def hypercube = queryResource.doQuery(constraints: [study: [clinicalData.longitudinalStudy.name]])
//        def result = hypercube*.value as Multiset
//        hypercube.loadDimensions()
//        def concepts = hypercube.dimensionElements(DimensionDescription.dimensionsMap.concept) as HashMultiset
//        def patients = hypercube.dimensionElements(DimensionDescription.dimensionsMap.patient) as HashMultiset
//
//        def expected = clinicalData.longitudinalClinicalFacts*.textValue as HashMultiset
//        def expectedConcepts = testData.conceptData.conceptDimensions as HashMultiset
//        def expectedPatients = clinicalData.patients as HashMultiset
//
//        expect:
//        result == expected
//        concepts == expectedConcepts
//        patients == expectedPatients
//    }

}
