/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import grails.test.mixin.integration.Integration
import grails.transaction.Transactional
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.hibernate.criterion.DetachedCriteria
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.transmartproject.core.dataquery.SortSpecification
import org.transmartproject.core.multidimquery.DataRetrievalParameters
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.StudyNameConstraint
import org.transmartproject.core.users.User
import org.transmartproject.db.Dictionaries
import org.transmartproject.db.TestData
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.querytool.QtQueryResultType
import org.transmartproject.mock.MockAdmin
import org.transmartproject.rest.hypercubeProto.ObservationsProto
import org.transmartproject.rest.serialization.HypercubeCSVSerializer
import org.transmartproject.rest.serialization.HypercubeJsonSerializer
import org.transmartproject.rest.serialization.HypercubeProtobufSerializer
import spock.lang.Ignore
import spock.lang.Specification

import java.util.zip.ZipOutputStream

import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that

/**
 * Created by piotrzakrzewski on 02/11/2016.
 */
@Integration
@DirtiesContext
@Transactional
@Slf4j
@Ignore // FIXME
class ObservationsBuilderSpec extends Specification {

    User adminUser

    @Autowired
    MultidimensionalDataResourceService queryResource

    @Autowired
    SessionFactory sessionFactory

    void setupData() {
        def session = sessionFactory.currentSession
        // Check if dictionaries were already loaded before
        def resultTypes = DetachedCriteria.forClass(QtQueryResultType).getExecutableCriteria(session).list() as List<QtQueryResultType>
        if (resultTypes.size() == 0) {
            def dictionaries = new Dictionaries()
            dictionaries.saveAll()
            def testData = TestData.createHypercubeDefault()
            testData.saveAll()
        }
        adminUser = new MockAdmin('admin')
    }

    void testJsonSerialization() {
        setupData()
        Constraint constraint = new StudyNameConstraint(studyId: 'longitudinal study')
        def args = new DataRetrievalParameters(constraint: constraint, sort: [new SortSpecification(dimension: 'patient')])
        def mockedCube = queryResource.retrieveData(args, 'clinical', adminUser)
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
        cells.size() == 18
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
    void testPackedDimsSerialization() {
        setupData()
        Constraint constraint = new StudyNameConstraint(studyId: 'multidimensional study')
        def args = new DataRetrievalParameters(constraint: constraint, sort: [new SortSpecification(dimension: 'patient')])
        def mockedCube = queryResource.retrieveData(args, 'clinical', adminUser)
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
            if (count > 18) {
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
        cells.size() == 18
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

    void testProtobufSerialization() {
        setupData()
        Constraint constraint = new StudyNameConstraint(studyId: 'longitudinal study')
        def args = new DataRetrievalParameters(constraint: constraint, sort: [new SortSpecification(dimension: 'patient')])
        def mockedCube = queryResource.retrieveData(args, 'clinical', adminUser)
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
            if (count >= 18) {
                throw new Exception("Expected previous message to be marked as 'last'. Found at least $count cells " +
                        "for 18 observations")
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
        header.sortList[0].dimensionIndex == header.dimensionDeclarationsList.findIndexOf { it.name == 'patient' }
        header.sortList[0].sortOrder.toString() == 'ASC'
        header.sortList[0].field == 0
        cells.size() == 18
        footer != null

        when:
        def PATIENT = queryResource.getDimension('patient')
        def fields = PATIENT.elementFields.keySet().asList()
        def patientDimIndex = header.dimensionDeclarationsList.findIndexOf { it.name == 'patient' }
        def idFieldIndex = header.dimensionDeclarationsList[patientDimIndex].fieldsList.findIndexOf { it.name == 'id' }
        def patientElement = footer.dimensionList[patientDimIndex]

        then:
        patientElement.fieldsList[idFieldIndex].intValueList as Set<Long> == [-101, -102, -103] as Set<Long>
    }

    void testCSVSerialization() {
        setupData()
        def dataType = 'clinical'
        def fileExtension = '.tsv'
        Constraint constraint = new StudyNameConstraint(studyId: 'longitudinal study')
        def args = new DataRetrievalParameters(constraint: constraint)
        def cube = queryResource.retrieveData(args, dataType, adminUser)
        def builder = new HypercubeCSVSerializer()

        when:
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()
        ZipOutputStream out = new ZipOutputStream(byteArrayOutputStream)
        builder.write([dataType : dataType], cube, out)
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

}
