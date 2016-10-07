package org.transmartproject.db.dataquery2

import com.google.common.collect.HashMultiset
import com.google.common.collect.Multiset
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.TestData
import org.transmartproject.db.TransmartSpecification
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.i2b2data.TrialVisit
import org.transmartproject.db.metadata.DimensionDescription
import spock.lang.Ignore

@Integration
@Rollback
class HypercubeIntegrationSpec extends TransmartSpecification {

    TestData testData
    ClinicalTestData clinicalData

    @Autowired
    MultidimensionalDataResourceService queryResource

    void setupData() {
        testData = TestData.createHypercubeDefault()
        clinicalData = testData.clinicalData
        testData.saveAll()
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

    @Ignore
    void 'devTest'() {
        setupData()

        def thename = clinicalData.longitudinalStudy.name

        def crit = TrialVisit.where {
            study {
                studyId in [thename]
            }
        }
        crit = ObservationFact.createCriteria()
        def res  = crit.where {
            trialVisit {
                study {
                    studyId in [thename]
                }
            }
        }.scroll()
        def o = crit.list()

        expect:
        clinicalData.longitudinalClinicalFacts[0] in o
    }

    @Ignore
    void devTest2() {
        setupData()

        def result = queryResource.doQuery(constraints: [study: [clinicalData.longitudinalStudy.name]])
        result

        expect:
        result as HashMultiset == clinicalData.longitudinalClinicalFacts*.textValue as HashMultiset
    }

    @Ignore
    void 'test_basic_longitudinal_retrieval'() {
        setupData()

        def hypercube = queryResource.doQuery(constraints: [study: [clinicalData.longitudinalStudy.name]])
        def result = hypercube*.value as HashMultiset
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
