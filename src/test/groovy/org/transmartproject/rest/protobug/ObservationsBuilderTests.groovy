package org.transmartproject.rest.protobug

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.multidimquery.PatientDimension
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.StudyNameConstraint
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.TestData
import org.transmartproject.rest.hypercubeProto.ObservationsProto
import org.transmartproject.rest.protobuf.ObservationsSerializer
import spock.lang.Specification

import static spock.util.matcher.HamcrestSupport.that
import static org.hamcrest.Matchers.*


/**
 * Created by piotrzakrzewski on 02/11/2016.
 */
@Integration
@Rollback
@Slf4j
class ObservationsBuilderTests extends Specification {

    TestData testData
    ClinicalTestData clinicalData
    Map<String, DimensionImpl> dims
    
    @Autowired
    MultidimensionalDataResourceService queryResource

    public void testJsonSerialization() {
        setupData()
        Constraint constraint = new StudyNameConstraint(studyId: clinicalData.longitudinalStudy.studyId)
        def mockedCube = queryResource.retrieveData('clinical', [clinicalData.longitudinalStudy], constraint: constraint)
        def builder = new ObservationsSerializer(mockedCube, ObservationsSerializer.Format.JSON, null)

        when:
        def out = new ByteArrayOutputStream()
        builder.write(out)
        out.flush()
        Collection result = new JsonSlurper().parse(out.toByteArray())
        def dimElementsSize = result.last()['dimension'].size()

        then:
        result.size() == 14
        that result, everyItem(anyOf(
                hasKey('dimensionDeclarations'),
                hasKey('dimensionIndexes'),
                hasKey('dimension')
        ))
        that result.first()['dimensionDeclarations'], hasSize(mockedCube.dimensions.size())
        that result.first()['dimensionDeclarations']['name'],
                containsInAnyOrder(mockedCube.dimensions.collect{it.toString()}.toArray()
                )
        that result['dimension'].findAll(), everyItem(hasSize(dimElementsSize))
        that result['dimensionIndexes'].findAll(), everyItem(hasSize(dimElementsSize))
    }

    public void testPackedDimsSerialization() {
        setupData()
        Constraint constraint = new StudyNameConstraint(studyId: clinicalData.multidimsStudy.studyId)
        def mockedCube = queryResource.retrieveData('clinical', [clinicalData.multidimsStudy], constraint: constraint)
        def patientDimension = DimensionDescription.dimensionsMap.patient
        def builder = new ObservationsSerializer(mockedCube, ObservationsSerializer.Format.JSON, patientDimension)

        when:
        def out = new ByteArrayOutputStream()
        builder.write(out)
        out.flush()
        Collection result = new JsonSlurper().parse(out.toByteArray())
        def dimElementsSize = result.last()['dimension'].size()
        def dimensionDeclarations = result.first()['dimensionDeclarations']
        def notPackedDimensions = dimensionDeclarations.findAll {it.inline && !it.packed }
        def notPackedDimensionsSize = notPackedDimensions.size()

        then:
        result.size() == 14
        that result, everyItem(anyOf(
                hasKey('dimensionDeclarations'),
                hasKey('dimensionIndexes'),
                hasKey('dimension')
        ))
        // declarations for all dimensions exist
        that dimensionDeclarations, hasSize(mockedCube.dimensions.size())
        that dimensionDeclarations['name'],
                containsInAnyOrder(mockedCube.dimensions.collect{it.toString()}.toArray()
                )
        // at least one declaration of packed dimension exists
        that dimensionDeclarations['packed'], hasItem(true)
        that result['stringValues'], hasSize(greaterThan(1))

        // indexes for all dense dimensions (dimension elements) exist
        that result['dimension'].findAll(), everyItem(hasSize(dimElementsSize))
        that result['dimensionIndexes'].findAll(), everyItem(hasSize(notPackedDimensionsSize))
    }

    public void testProtobufSerialization() {
        setupData()
        Constraint constraint = new StudyNameConstraint(studyId: clinicalData.longitudinalStudy.studyId)
        def mockedCube = queryResource.retrieveData('clinical', [clinicalData.longitudinalStudy], constraint: constraint)
        def builder = new ObservationsSerializer(mockedCube, ObservationsSerializer.Format.PROTOBUF, null)

        when:
        def s_out = new ByteArrayOutputStream()
        builder.write(s_out)
        s_out.flush()
        def data = s_out.toByteArray()

        then:
        data != null
        data.length > 0

        when:
        def s_in = new ByteArrayInputStream(data)
        log.info "Reading header..."
        def header = ObservationsProto.Header.parseDelimitedFrom(s_in)
        def cells = []
        int count = 0
        while(true) {
            count++
            if (count > 12) {
                throw new Exception("Expected previous message to be marked as 'last'.")
            }
            log.info "Reading cell..."
            def cell = ObservationsProto.Observation.parseDelimitedFrom(s_in)
            cells << cell
            if (cell.last) {
                log.info "Last cell."
                break
            }
        }
        log.info "Reading footer..."
        def footer = ObservationsProto.Footer.parseDelimitedFrom(s_in)

        then:
        header != null
        header.dimensionDeclarationsList.size() == mockedCube.dimensions.size()
        cells.size() == 12
        footer != null
    }

    void setupData() {
        testData = TestData.createHypercubeDefault()
        clinicalData = testData.clinicalData
        testData.saveAll()
        dims = DimensionDescription.dimensionsMap
    }
}
