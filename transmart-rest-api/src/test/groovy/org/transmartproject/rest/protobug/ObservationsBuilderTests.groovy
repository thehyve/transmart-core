/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest.protobug

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.users.User
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.StudyNameConstraint
import org.transmartproject.db.TestData
import org.transmartproject.db.user.AccessLevelTestData
import org.transmartproject.rest.hypercubeProto.ObservationsProto
import org.transmartproject.rest.serialization.DataTableSerializer
import org.transmartproject.rest.serialization.HypercubeCSVSerializer
import org.transmartproject.rest.serialization.HypercubeProtobufSerializer
import org.transmartproject.rest.serialization.HypercubeJsonSerializer
import spock.lang.Ignore
import spock.lang.Specification

import java.util.zip.ZipOutputStream

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
    AccessLevelTestData accessLevelTestData
    User adminUser

    @Autowired
    MultidimensionalDataResourceService queryResource


    void setupData() {
        TestData.clearAllData()

        testData = TestData.createHypercubeDefault()
        clinicalData = testData.clinicalData
        testData.saveAll()
        accessLevelTestData = new AccessLevelTestData()
        accessLevelTestData.saveAuthorities()
        adminUser = accessLevelTestData.users[0]
    }

    public void testJsonSerialization() {
        setupData()
        Constraint constraint = new StudyNameConstraint(studyId: clinicalData.longitudinalStudy.studyId)
        def mockedCube = queryResource.retrieveData('clinical', adminUser,
                constraint: constraint, sort: ['patient'])
        def builder = new HypercubeJsonSerializer()

        when:
        def out = new ByteArrayOutputStream()
        builder.write(mockedCube, out)
        out.flush()
        def result = new JsonSlurper().parse(out.toByteArray())
        def declarations = result.dimensionDeclarations
        def sort = result.sort
        def cells = result.cells
        def dimensionElements = result.dimensionElements
        def dimElementsSize = mockedCube.dimensions.findAll { it.density.isDense }.size()

        then:
        cells.size() == clinicalData.longitudinalClinicalFacts.size()
        that cells, everyItem(hasKey('dimensionIndexes'))
        declarations != null
        declarations.size() == mockedCube.dimensions.size()
        that declarations*.name, containsInAnyOrder(mockedCube.dimensions.collect{it.name}.toArray())
        sort.size() > 0
        sort[0] == [dimension: 'patient', sortOrder: 'asc']
        dimensionElements != null
        dimensionElements.size() == dimElementsSize
        that cells['dimensionIndexes'].findAll(), everyItem(hasSize(dimElementsSize))
    }

    @Ignore("packing is not yet implemented in the serializer")
    public void testPackedDimsSerialization() {
        setupData()
        Constraint constraint = new StudyNameConstraint(studyId: clinicalData.multidimsStudy.studyId)
        def mockedCube = queryResource.retrieveData('clinical', adminUser,
                constraint: constraint, sort: ['patient'])
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
        def firstSort = header.sortList[0]

        then:
        cells.size() == clinicalData.multidimsClinicalFacts.size()
        // declarations for all dimensions exist
        that dimensionDeclarations, hasSize(mockedCube.dimensions.size())
        that dimensionDeclarations['name'],
                containsInAnyOrder(mockedCube.dimensions.collect{it.toString()}.toArray()
                )
        // sort order declaration for 'patient' exists
        firstSort.dimensionIndex == dimensionDeclarations.indexOf { it.name == 'patient' }
        firstSort.sortOrder.toString() == 'ASC'
        firstSort.field == 0
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
        def mockedCube = queryResource.retrieveData('clinical', adminUser,
                constraint: constraint, sort: ['patient'])
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
        println "PROTOBUF TEST: Clinical facts: ${clinicalData.longitudinalClinicalFacts.toListString()}"

        then:
        header != null
        header.dimensionDeclarationsList.size() == mockedCube.dimensions.size()
        header.sortList[0].dimensionIndex == header.dimensionDeclarationsList.findIndexOf { it.name == 'patient' }
        header.sortList[0].sortOrder.toString() == 'ASC'
        header.sortList[0].field == 0
        cells.size() == clinicalData.longitudinalClinicalFacts.size()
        footer != null
    }

    public void testCSVSerialization() {
        setupData()
        def dataType = 'clinical'
        def fileExtension = '.tsv'
        Constraint constraint = new StudyNameConstraint(studyId: clinicalData.longitudinalStudy.studyId)
        def mockedCube = queryResource.retrieveData(dataType, adminUser, constraint: constraint)
        def builder = new HypercubeCSVSerializer()

        when:
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()
        ZipOutputStream out = new ZipOutputStream(byteArrayOutputStream)
        builder.write([dataType : dataType], mockedCube, out)
        out.close()
        out.flush()
        List expectedEntries = ["${dataType}_observations$fileExtension",
                                "${dataType}_concept$fileExtension",
                                "${dataType}_patient$fileExtension",
                                "${dataType}_study$fileExtension",
                                "${dataType}_trial_visit$fileExtension"]

        then:
        out.xentries != null
        out.names.sort() == expectedEntries.sort()
    }

    void testWideFormatCSVSerialization() {
        setupData()
        def dataType = 'clinical'
        def fileExtension = '.tsv'
        Constraint constraint = new StudyNameConstraint(studyId: clinicalData.longitudinalStudy.studyId)
        def mockedCube = queryResource.retrieveData(dataType, adminUser, constraint: constraint)
        def builder = new HypercubeCSVSerializer()

        when:
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()
        ZipOutputStream out = new ZipOutputStream(byteArrayOutputStream)
        builder.write([dataType : dataType], mockedCube, out)
        out.close()
        out.flush()
        List expectedEntries = ["${dataType}_observations$fileExtension",
                                "${dataType}_concept$fileExtension",
                                "${dataType}_patient$fileExtension",
                                "${dataType}_study$fileExtension",
                                "${dataType}_trial_visit$fileExtension"]

        then:
        out.xentries != null
        out.names.sort() == expectedEntries.sort()
    }

    void testDataTableSerialization() {
        setupData()
        def dataType = 'clinical'
        def rowDimensions = ['patient', 'study']
        def columnDimensions = ['concept', 'trial visit']
        def limit = 10
        Constraint constraint = new StudyNameConstraint(studyId: clinicalData.longitudinalStudy.studyId)
        def mockedDataTable = queryResource.retrieveDataTable(dataType, constraint, adminUser,
                rowDimensions: rowDimensions, columnDimensions: columnDimensions, sort: ['patient'], limit: limit)

        when:
        def out = new ByteArrayOutputStream()
        DataTableSerializer.write(mockedDataTable, out)
        out.flush()
        def result = new JsonSlurper().parse(out.toByteArray())
        def rows = result.rows
        def offset = result.offset
        def sorting = result.sorting
        def columnHeaders = result.column_headers
        def columnDim = result.column_dimensions
        def rowDim = result.row_dimensions

        then:
        rows.size() < clinicalData.longitudinalClinicalFacts.size()
        rows.size() <= limit
        offset == 0

        sorting.size() == columnDimensions.size() + rowDimensions.size()
        sorting[0] == [dimension: 'patient', order: 'asc']
        sorting[1] == [dimension: 'study', order: 'asc']
        sorting[2] == [dimension: 'concept', order: 'asc']
        sorting[3] == [dimension: 'trial visit', order: 'asc']

        columnHeaders*.dimension == columnDimensions
        columnHeaders[0].keys == ['c5', 'c5', 'c5', 'c6', 'c6', 'c6']
        columnHeaders[1].keys == [4, 5, 6, 4, 5, 6]

        columnDim*.name == columnDimensions
        columnDim[0].elements.size() == (columnHeaders[0].keys as Set).size()
        columnDim[1].elements.size() == (columnHeaders[1].keys as Set).size()

        rowDim*.name == rowDimensions
        rowDim[0].elements.size() == 3
        rowDim[1].elements.size() == 1

        that rows*.dimensions, everyItem(hasSize(2))
        that rows*.dimensions*.dimension, everyItem(contains('patient', 'study'))
        (rows*.dimensions.collect{it[1].key} as Set).size() == rowDim[1].elements.size()
        that rows*.row, everyItem(hasSize(6))
        (rows*.dimensions.collect{it[0].key} as Set).size() == rowDim[0].elements.size()

    }

}
