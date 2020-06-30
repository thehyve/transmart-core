package org.transmartproject.db.multidimquery

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.transmartproject.db.TestData
import spock.lang.Specification
import org.transmartproject.db.dataquery.clinical.ClinicalTestData

@Rollback
@Integration
class StudyIntegrationSpec extends Specification {

    TestData testData
    ClinicalTestData clinicalData

    void setupData() {
        TestData.prepareCleanDatabase()

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
