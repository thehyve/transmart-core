package org.transmartproject.db.multidimquery

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.transmartproject.db.TestData
import org.transmartproject.db.TransmartSpecification
import org.transmartproject.db.dataquery.clinical.ClinicalTestData

@Rollback
@Integration
class StudyIntegrationSpec extends TransmartSpecification {

    TestData testData
    ClinicalTestData clinicalData

    void setupData() {
        testData = TestData.createHypercubeDefault()
        clinicalData = testData.clinicalData
        clinicalData.saveAll()
    }

    void 'test study dimensions'() {
        setupData()

        expect:
        clinicalData.ehrStudy.getDimensionByName('visit') == DimensionImpl.VISIT
        DimensionImpl.TRIAL_VISIT in clinicalData.longitudinalStudy.dimensions
    }

}
