package org.transmartproject.rest.protobug

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.dataquery2.DimensionImpl
import org.transmartproject.db.dataquery2.HypercubeValueImpl
import org.transmartproject.db.dataquery2.query.Constraint
import org.transmartproject.db.dataquery2.query.StudyConstraint
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
    Map<String, DimensionImpl> dims
    
    @Autowired
    MultidimensionalDataResourceService queryResource

    @Test
    public void testSerialization() throws Exception {
        setupData()
        Constraint constraint = new StudyConstraint(studyId: clinicalData.longitudinalStudy.studyId)
        def mockedCube = queryResource.retrieveData('clinical', [clinicalData.longitudinalStudy], constraint: constraint)
        def builder = new ObservationsSerializer(mockedCube)
        def blob = builder.getDimensionsDefs()
        assertThat(blob, notNullValue())
        Iterator<HypercubeValueImpl> it = mockedCube.iterator
        def cellMsgs = []
        while (it.hasNext()) {
            HypercubeValueImpl value = it.next()
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
