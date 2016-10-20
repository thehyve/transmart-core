package org.transmartproject.db.dataquery2

import com.google.common.collect.HashMultiset
import com.google.common.collect.Multiset
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.TestData
import org.transmartproject.db.TransmartSpecification
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.metadata.DimensionDescription

class HypercubeIntegrationSpec extends TransmartSpecification {

    TestData testData
    ClinicalTestData clinicalData

    @Autowired
    HQueryResourceService queryResource

    void setup() {
        testData = TestData.createDefault()
        clinicalData = testData.clinicalData
        testData.saveAll()
    }

    void testTest() {
        def it = method(1,1)

        expect:
        it == 2
    }

    int method(a, b) {
        return a+b
    }


    void 'test basic longitudinal retrieval'() {

        def hypercube = queryResource.doQuery(constraints: [study: [clinicalData.longitudinalStudy.name]])
        def result = hypercube*.value as Multiset
        hypercube.loadDimensions()
        def concepts = hypercube.dimensionElements(DimensionDescription.dimensionsMap.concept) as HashMultiset
        def patients = hypercube.dimensionElements(DimensionDescription.dimensionsMap.patient) as HashMultiset

        def expected = clinicalData.longitudinalClinicalFacts*.textValue as HashMultiset
        def expectedConcepts = testData.conceptData.conceptDimensions as HashMultiset
        def expectedPatients = clinicalData.patients as HashMultiset

        expect:
        result == expected
        concepts == expectedConcepts
        patients == expectedPatients
    }

}
