package org.transmartproject.db.dataquery.highdim.rnaseq

import com.google.common.collect.Lists
import grails.test.mixin.TestMixin
import org.hibernate.SessionFactory
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.rnaseq.RnaSeqValues
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.chromoregion.Region
import org.transmartproject.core.dataquery.highdim.chromoregion.RegionRow
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.dataquery.highdim.DeGplInfo
import org.transmartproject.db.dataquery.highdim.chromoregion.DeChromosomalRegion
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.db.dataquery.highdim.rnaseq.RnaSeqModule.RNASEQ_VALUES_PROJECTION
import static org.transmartproject.db.dataquery.highdim.rnaseq.RnaSeqTestData.TRIAL_NAME
import static org.transmartproject.db.test.Matchers.hasSameInterfaceProperties

@TestMixin(RuleBasedIntegrationTestMixin)
class RnaSeqEndToEndRetrievalTests {

    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource<RegionRow> rnaseqResource

    TabularResult<AssayColumn, RegionRow> dataQueryResult

    Projection<RnaSeqValues> projection

    SessionFactory sessionFactory

    RnaSeqTestData testData = new RnaSeqTestData()

    @Before
    void setUp() {
        testData.saveAll()
        sessionFactory.currentSession.flush()

        rnaseqResource = highDimensionResourceService.getSubResourceForType 'rnaseq'

        /* projection never varies in our tests */
        projection = rnaseqResource.createProjection([:], RNASEQ_VALUES_PROJECTION)
    }

    @After
    void tearDown() {
        dataQueryResult?.close()
    }

    @Test
    void basicTest() {
        def assayConstraints = [
                rnaseqResource.createAssayConstraint(
                        AssayConstraint.TRIAL_NAME_CONSTRAINT, name: TRIAL_NAME),
                rnaseqResource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: testData.allPatientsQueryResult.id),
        ]
        def dataConstraints = []

        dataQueryResult = rnaseqResource.retrieveData assayConstraints, dataConstraints, projection

        assertThat dataQueryResult, allOf(
                is(notNullValue()),
                hasProperty('indicesList', contains(
                        /* they're ordered by assay id */
                        hasSameInterfaceProperties(Assay, testData.assays[1], ['platform']),
                        hasSameInterfaceProperties(Assay, testData.assays[0], ['platform']),
                )),
                hasProperty('rowsDimensionLabel', equalTo('Regions')),
                hasProperty('columnsDimensionLabel', equalTo('Sample codes')),
        )

        List<AssayColumn> assayColumns = dataQueryResult.indicesList

        Iterator<RegionRow> rows = dataQueryResult.rows
        def regionRows = Lists.newArrayList(rows)

        assertThat regionRows, hasSize(2)
        /* results are ordered (asc) by region id */
        assertThat regionRows[0], allOf(
                hasSameInterfaceProperties(Region, testData.regions[1], ['platform']),
                hasProperty('label', equalTo(testData.regions[1].cytoband)))
        assertThat regionRows[1], allOf(
                hasSameInterfaceProperties(Region, testData.regions[0], ['platform']),
                hasProperty('label', equalTo(testData.regions[0].cytoband)))

        assertThat regionRows[1][assayColumns[1]],
                hasSameInterfaceProperties(RnaSeqValues, testData.rnaseqData[0])
        assertThat regionRows[1][assayColumns[0]],
                hasSameInterfaceProperties(RnaSeqValues, testData.rnaseqData[1])
        assertThat regionRows[0][assayColumns[1]],
                hasSameInterfaceProperties(RnaSeqValues, testData.rnaseqData[2])
        assertThat regionRows[0][assayColumns[0]],
                hasSameInterfaceProperties(RnaSeqValues, testData.rnaseqData[3])
    }

    @Test
    void testSegments_meetOne() {
        def assayConstraints = [
                rnaseqResource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: testData.allPatientsQueryResult.id),
        ]
        def dataConstraints = [
                // start matches start of regions[0]
                rnaseqResource.createDataConstraint(
                        DataConstraint.CHROMOSOME_SEGMENT_CONSTRAINT,
                        chromosome: '1', start: 33, end: 44
                )
        ]

        dataQueryResult = rnaseqResource.retrieveData assayConstraints, dataConstraints, projection

        def regionRows = Lists.newArrayList(dataQueryResult.rows)

        assertThat regionRows, hasSize(1)
        assertThat regionRows[0], hasSameInterfaceProperties(
                Region, testData.regions[0], [ 'platform' ])
    }

    @Test
    void testSegments_meetBoth() {
        def assayConstraints = [
                rnaseqResource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: testData.allPatientsQueryResult.id),
        ]
        def dataConstraints = [
                // start matches start of regions[0]
                rnaseqResource.createDataConstraint(
                        DataConstraint.DISJUNCTION_CONSTRAINT,
                        subconstraints: [
                                (DataConstraint.CHROMOSOME_SEGMENT_CONSTRAINT): [
                                        /* test region wider then the segment */
                                        [ chromosome: '1', start: 44, end: 8888 ],
                                        /* segment aligned at the end of test region;
                                         *segment shorter than region */
                                        [ chromosome: '2', start: 88, end: 99 ],
                                ]
                        ]
                )
        ]

        def anotherPlatform = new DeGplInfo(
                title:          'Another Test Region Platform',
                organism:       'Homo Sapiens',
                annotationDate: Date.parse('yyyy-MM-dd', '2013-08-03'),
                markerType:     'Chromosomal',
                releaseNumber:  'hg19',
        )
        anotherPlatform.id = 'test-another-platform'
        anotherPlatform.save failOnError: true, flush: true

        // this region should not appear in the result set
        def anotherRegion = new DeChromosomalRegion(
                platform:       anotherPlatform,
                chromosome:     '1',
                start:          1,
                end:            10,
                numberOfProbes: 42,
                name:           'region 1:1-10'
        )
        anotherRegion.id = -2010L
        anotherRegion.save failOnError: true, flush: true

        dataQueryResult = rnaseqResource.retrieveData assayConstraints, dataConstraints, projection

        def regionRows = Lists.newArrayList(dataQueryResult.rows)

        assertThat regionRows, hasSize(2)
        assertThat regionRows, contains(
                hasSameInterfaceProperties(
                        Region, testData.regions[1], [ 'platform' ]),
                hasSameInterfaceProperties(
                        Region, testData.regions[0], [ 'platform' ]))
    }

    @Test
    void testSegments_meetNone() {
        def assayConstraints = [
                rnaseqResource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: testData.allPatientsQueryResult.id),
        ]
        def dataConstraints = [
                // start matches start of regions[0]
                rnaseqResource.createDataConstraint(
                        DataConstraint.DISJUNCTION_CONSTRAINT,
                        subconstraints: [
                                (DataConstraint.CHROMOSOME_SEGMENT_CONSTRAINT): [
                                        [ chromosome: 'X' ],
                                        [ chromosome: '1', start: 1,   end: 32 ],
                                        [ chromosome: '2', start: 100, end: 1000 ],
                                ]
                        ]
                )
        ]

        dataQueryResult = rnaseqResource.retrieveData assayConstraints, dataConstraints, projection

        assertThat dataQueryResult, hasProperty('indicesList', is(not(empty())))
        assertThat Lists.newArrayList(dataQueryResult.rows), is(empty())
    }

}
