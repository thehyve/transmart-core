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
        //TODO: better checks
        result == expected
        concepts == expectedConcepts
        patients == expectedPatients
        trialVisits == expectedVisits
    }

}
