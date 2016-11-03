package org.transmartproject.rest.protobug

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.dataquery2.Dimension
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.TestData
import org.transmartproject.rest.protobuf.ObservationsSerializer

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.core.IsNull.notNullValue



/**
 * Created by piotrzakrzewski on 02/11/2016.
 */
@Integration
@Rollback
class ObservationsBuilderTests {


    TestData testData
    ClinicalTestData clinicalData
    Map<String,Dimension> dims


    @Autowired
    MultidimensionalDataResourceService queryResource

    @Test
    public void testSerialization() throws Exception {
        setupData()
        new ObservationsSerializer()
        //def mockedCube= queryResource.doQuery(constraints: [study: [clinicalData.longitudinalStudy.name]])
        //def blob =  observationsBuilder.getDimensionsDefs(mockedCube)
        //assertThat(blob, notNullValue())
    }

    void setupData() {
        testData = TestData.createHypercubeDefault()
        clinicalData = testData.clinicalData
        testData.saveAll()
        dims = DimensionDescription.dimensionsMap
    }
}
