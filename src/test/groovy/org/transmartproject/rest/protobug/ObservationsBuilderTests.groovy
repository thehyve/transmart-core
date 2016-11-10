package org.transmartproject.rest.protobug

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.dataquery2.Dimension
import org.transmartproject.db.dataquery2.HypercubeValue
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.TestData
import org.transmartproject.rest.protobuf.ObservationsProto
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
    Map<String, Dimension> dims


    @Autowired
    MultidimensionalDataResourceService queryResource

    @Test
    public void testSerialization() throws Exception {
        setupData()
        def mockedCube = queryResource.doQuery(constraints: [study: [clinicalData.longitudinalStudy.studyId]])
        def builder = new ObservationsSerializer(mockedCube)
        def blob = builder.getDimensionsDefs()
        assertThat(blob, notNullValue())
        Iterator<HypercubeValue> it = mockedCube.iterator
        def cellMsgs = []
        while (it.hasNext()) {
            HypercubeValue value = it.next()
            cellMsgs.add(builder.createCell(value))
        }
        assertThat(cellMsgs, notNullValue())
        def footer = builder.getFooter()
        assertThat(footer, notNullValue())
    }

    void setupData() {
        testData = TestData.createHypercubeDefault()
        clinicalData = testData.clinicalData
        testData.saveAll()
        dims = DimensionDescription.dimensionsMap
    }
}
