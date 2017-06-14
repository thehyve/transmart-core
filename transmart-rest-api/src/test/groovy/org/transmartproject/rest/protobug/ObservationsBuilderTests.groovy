/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest.protobug

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.StudyNameConstraint
import org.transmartproject.db.TestData
import org.transmartproject.rest.hypercubeProto.ObservationsProto
import org.transmartproject.rest.serialization.HypercubeCSVSerializer
import org.transmartproject.rest.serialization.HypercubeProtobufSerializer
import org.transmartproject.rest.serialization.HypercubeJsonSerializer
import spock.lang.Ignore
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

    String tempDirectory = '/var/tmp/jobs/test'

    @Autowired
    MultidimensionalDataResourceService queryResource

    public void testJsonSerialization() {
        setupData()
        Constraint constraint = new StudyNameConstraint(studyId: clinicalData.longitudinalStudy.studyId)
        def mockedCube = queryResource.retrieveData('clinical', [clinicalData.longitudinalStudy], constraint: constraint)
        def builder = new HypercubeJsonSerializer()

        when:
        def out = new ByteArrayOutputStream()
        builder.write(mockedCube, out)
        out.flush()
        def result = new JsonSlurper().parse(out.toByteArray())
        def declarations = result.dimensionDeclarations
        def cells = result.cells
        def dimensionElements = result.dimensionElements
        def dimElementsSize = mockedCube.dimensions.findAll { it.density.isDense }.size()

        then:
        cells.size() == clinicalData.longitudinalClinicalFacts.size()
        that cells, everyItem(hasKey('dimensionIndexes'))
        declarations != null
        declarations.size() == mockedCube.dimensions.size()
        that declarations*.name, containsInAnyOrder(mockedCube.dimensions.collect{it.name}.toArray())
        dimensionElements != null
        dimensionElements.size() == dimElementsSize
        that cells['dimensionIndexes'].findAll(), everyItem(hasSize(dimElementsSize))
    }

    @Ignore("packing is not yet implemented in the serializer")
    public void testPackedDimsSerialization() {
        setupData()
        Constraint constraint = new StudyNameConstraint(studyId: clinicalData.multidimsStudy.studyId)
        def mockedCube = queryResource.retrieveData('clinical', [clinicalData.multidimsStudy], constraint: constraint)
        def patientDimension = DimensionImpl.PATIENT
        def builder = new HypercubeProtobufSerializer()

        when:
        def s_out = new ByteArrayOutputStream()
        builder.write(mockedCube, s_out, packedDimension: patientDimension)
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
        def builder = new HypercubeProtobufSerializer()

        when:
        def s_out = new ByteArrayOutputStream()
        builder.write(mockedCube, s_out)
        s_out.flush()
        def data = s_out.toByteArray()

        then:
        data != null
        data.length > 0

        when:
        def s_in = new ByteArrayInputStream(data)
        log.info "Reading header..."
        def header = ObservationsProto.Header.parseDelimitedFrom(s_in)
        def last = header.last
        def cells = []
        int count = 0
        while(!last) {
            if (count >= clinicalData.longitudinalClinicalFacts.size()) {
                throw new Exception("Expected previous message to be marked as 'last'. Found at least $count cells " +
                        "for ${clinicalData.longitudinalClinicalFacts.size()} observations")
            }
            log.info "Reading cell..."
            def cell = ObservationsProto.Cell.parseDelimitedFrom(s_in)
            cells << cell
            count++
            last = cell.last
            if (last) {
                log.info "Last cell."
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

    public void testCSVSerialization() {
        setupData()
        def dataType = 'clinical'
        def fileExtension = '.tsv'
        Constraint constraint = new StudyNameConstraint(studyId: clinicalData.longitudinalStudy.studyId)
        def mockedCube = queryResource.retrieveData(dataType, [clinicalData.longitudinalStudy], constraint: constraint)
        def builder = new HypercubeCSVSerializer()
        File tempFile = new File(tempDirectory)
        tempFile.mkdirs()

        when:
        builder.write([directory: tempFile, dataType: dataType], mockedCube, null)
        File observations = new File(tempDirectory, "observations_$dataType$fileExtension")
        File concept = new File(tempDirectory, "concept_$dataType$fileExtension")
        File patient = new File(tempDirectory, "patient_$dataType$fileExtension")
        File study = new File(tempDirectory, "study_$dataType$fileExtension")
        File trial_visit = new File(tempDirectory, "trial_visit_$dataType$fileExtension")

        then:
        observations.exists()
        observations.isFile()

        concept.exists()
        concept.isFile()

        patient.exists()
        patient.isFile()

        study.exists()
        study.isFile()

        trial_visit.exists()
        trial_visit.isFile()
    }

    void setupData() {
        testData = TestData.createHypercubeDefault()
        clinicalData = testData.clinicalData
        testData.saveAll()
        //dims = DimensionImpl.dimensionsMap
    }

    def cleanup() {
        def tempDir = new File(tempDirectory)
        tempDir.deleteDir()
    }
}
