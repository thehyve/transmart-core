package org.transmartproject.rest.protobug

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.StudyNameConstraint
import org.transmartproject.db.TestData
import org.transmartproject.rest.hypercubeProto.ObservationsProto
import org.transmartproject.rest.serialization.JsonObservationsSerializer
import org.transmartproject.rest.serialization.ProtobufObservationsSerializer
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
    //Map<String, DimensionImpl> dims
    
    @Autowired
    MultidimensionalDataResourceService queryResource

    public void testJsonSerialization() {
        setupData()
        Constraint constraint = new StudyNameConstraint(studyId: clinicalData.longitudinalStudy.studyId)
        def mockedCube = queryResource.retrieveData('clinical', [clinicalData.longitudinalStudy], constraint: constraint)
        def builder = new JsonObservationsSerializer(mockedCube)

        when:
        def out = new ByteArrayOutputStream()
        builder.write(out)
        out.flush()
        def result = new JsonSlurper().parse(out.toByteArray())
        def header = result.header
        def cells = result.cells
        def footer = result.footer
        def dimElementsSize = mockedCube.dimensions.findAll { it.density.isDense }.size()

        then:
        cells.size() == clinicalData.longitudinalClinicalFacts.size()
        that cells, everyItem(hasKey('dimensionIndexes'))
        that header, hasKey('dimensionDeclarations')
        that header.dimensionDeclarations, hasSize(mockedCube.dimensions.size())
        that header.dimensionDeclarations['name'],
                containsInAnyOrder(mockedCube.dimensions.collect{it.toString()}.toArray()
                )
        that footer, hasKey('dimensions')
        that footer.dimensions, hasSize(dimElementsSize)
        that cells['dimensionIndexes'].findAll(), everyItem(hasSize(dimElementsSize))
    }

    public void testPackedDimsSerialization() {
        setupData()
        Constraint constraint = new StudyNameConstraint(studyId: clinicalData.multidimsStudy.studyId)
        def mockedCube = queryResource.retrieveData('clinical', [clinicalData.multidimsStudy], constraint: constraint)
        def patientDimension = DimensionImpl.PATIENT
        def builder = new ProtobufObservationsSerializer(mockedCube, patientDimension)

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
            if (count > clinicalData.multidimsClinicalFacts.size()) {
                throw new Exception("Expected previous message to be marked as 'last'.")
            }
            log.info "Reading cell..."
            def cell = ObservationsProto.PackedObservation.parseDelimitedFrom(s_in)
            cells << cell
            if (cell.last) {
                log.info "Last cell."
                break
            }
        }
        log.info "Reading footer..."
        ObservationsProto.Footer.parseDelimitedFrom(s_in)

        def dimensionDeclarations = header.dimensionDeclarationsList
        def inlinedDimensionsSize = dimensionDeclarations.findAll { it.inline }.size()
        def notPackedDimensions = dimensionDeclarations.findAll { !it.inline && !it.packed }
        def notPackedDimensionsSize = notPackedDimensions.size()

        then:
        cells.size() == clinicalData.multidimsClinicalFacts.size()
        // declarations for all dimensions exist
        that dimensionDeclarations, hasSize(mockedCube.dimensions.size())
        that dimensionDeclarations['name'],
                containsInAnyOrder(mockedCube.dimensions.collect{it.toString()}.toArray()
                )
        // at least one declaration of packed dimension exists
        that dimensionDeclarations['packed'], hasItem(true)
        that cells['stringValuesList'], hasSize(greaterThan(1))

        // indexes for all dense dimensions (dimension elements) exist
        that cells['inlineDimensionsList'].findAll(), everyItem(hasSize(inlinedDimensionsSize))
        that cells['dimensionIndexesList'].findAll(), everyItem(hasSize(notPackedDimensionsSize))
    }

    public void testProtobufSerialization() {
        setupData()
        Constraint constraint = new StudyNameConstraint(studyId: clinicalData.longitudinalStudy.studyId)
        def mockedCube = queryResource.retrieveData('clinical', [clinicalData.longitudinalStudy], constraint: constraint)
        def builder = new ProtobufObservationsSerializer(mockedCube, null)

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
            if (count > clinicalData.longitudinalClinicalFacts.size()) {
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
        cells.size() == clinicalData.longitudinalClinicalFacts.size()
        footer != null
    }

    void setupData() {
        testData = TestData.createHypercubeDefault()
        clinicalData = testData.clinicalData
        testData.saveAll()
        //dims = DimensionImpl.dimensionsMap
    }
}
