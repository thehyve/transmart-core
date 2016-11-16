package org.transmartproject.rest.protobug

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import groovy.json.JsonSlurper
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.dataquery2.DimensionImpl
import org.transmartproject.db.dataquery2.query.Constraint
import org.transmartproject.db.dataquery2.query.StudyConstraint
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.TestData
import org.transmartproject.rest.protobuf.ObservationsSerializer

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*


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
    public void testJsonSerialization() throws Exception {
        setupData()
        Constraint constraint = new StudyConstraint(studyId: clinicalData.longitudinalStudy.studyId)
        def mockedCube = queryResource.retrieveData('clinical', [clinicalData.longitudinalStudy], constraint: constraint)
        def builder = new ObservationsSerializer(mockedCube, ObservationsSerializer.Format.JSON)

        def out = new ByteArrayOutputStream()
        builder.write(out)
        out.flush()
        Collection result = new JsonSlurper().parse(out.toByteArray())
        def dimElementsSize = result.last()['dimension'].size()

        assertThat result.size(), is(14)
        assertThat result, contains(
                hasKey('dimensionDeclarations'),
                hasKey('dimensionIndexes'),
                hasKey('dimensionIndexes'),
                hasKey('dimensionIndexes'),
                hasKey('dimensionIndexes'),
                hasKey('dimensionIndexes'),
                hasKey('dimensionIndexes'),
                hasKey('dimensionIndexes'),
                hasKey('dimensionIndexes'),
                hasKey('dimensionIndexes'),
                hasKey('dimensionIndexes'),
                hasKey('dimensionIndexes'),
                hasKey('dimensionIndexes'),
                hasKey('dimension')
        )
        assertThat result.first()['dimensionDeclarations'], hasSize(mockedCube.dimensions.size())
        assertThat result.first()['dimensionDeclarations']['name'],
                containsInAnyOrder(mockedCube.dimensions.collect{it.toString()}.toArray()
                )
        assertThat result['dimension'].findAll(), everyItem(hasSize(dimElementsSize))
        assertThat result['dimensionIndexes'].findAll(), everyItem(hasSize(dimElementsSize))
    }

    @Test
    public void testProtobufSerialization() throws Exception {
        setupData()
        Constraint constraint = new StudyConstraint(studyId: clinicalData.longitudinalStudy.studyId)
        def mockedCube = queryResource.retrieveData('clinical', [clinicalData.longitudinalStudy], constraint: constraint)
        def builder = new ObservationsSerializer(mockedCube, ObservationsSerializer.Format.PROTOBUF)

        def out = new ByteArrayOutputStream()
        builder.write(out)
        out.flush()

        assertThat out.toByteArray(), not(empty())
        assertThat mockedCube.dimensions.each{out.toString().contains(it.toString())}, hasSize(mockedCube.dimensions.size())
    }

    void setupData() {
        testData = TestData.createHypercubeDefault()
        clinicalData = testData.clinicalData
        testData.saveAll()
        dims = DimensionDescription.dimensionsMap
    }
}
