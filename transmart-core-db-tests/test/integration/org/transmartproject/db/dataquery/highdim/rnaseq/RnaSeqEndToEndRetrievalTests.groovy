/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.dataquery.highdim.rnaseq

import com.google.common.collect.Lists
import grails.test.mixin.TestMixin
import groovy.test.GroovyAssert
import org.hibernate.SessionFactory
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.chromoregion.Region
import org.transmartproject.core.dataquery.highdim.chromoregion.RegionRow
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.dataquery.highdim.rnaseq.RnaSeqValues
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.querytool.ConstraintByOmicsValue
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

    private static final double DELTA = 0.0001
    private static final String concept_code = 'concept code #1'

    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource<RegionRow> rnaseqResource

    TabularResult<AssayColumn, RegionRow> dataQueryResult

    Projection<RnaSeqValues> projection

    SessionFactory sessionFactory

    RnaSeqTestData testData = new RnaSeqTestData()

    AssayConstraint trialNameConstraint

    String conceptKey

    @Before
    void setUp() {
        testData.saveAll()
        sessionFactory.currentSession.flush()

        rnaseqResource = highDimensionResourceService.getSubResourceForType 'rnaseq'

        trialNameConstraint = rnaseqResource.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: RnaSeqTestData.TRIAL_NAME)

        projection = rnaseqResource.createProjection([:], RNASEQ_VALUES_PROJECTION)

        conceptKey = '\\\\' + testData.concept.tableAccesses[0].tableCode + testData.concept.conceptDimensions[0].conceptPath
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
                hasProperty('label', equalTo(testData.regions[1].name)),
                hasProperty('bioMarker', equalTo(testData.regions[1].geneSymbol)),
                hasProperty('platform', allOf(
                        hasProperty('id', equalTo(testData.regionPlatform.id)),
                        hasProperty('title', equalTo(testData.regionPlatform.title)),
                        hasProperty('organism', equalTo(testData.regionPlatform.organism)),
                        hasProperty('annotationDate', equalTo(testData.regionPlatform.annotationDate)),
                        hasProperty('markerType', equalTo(testData.regionPlatform.markerType)),
                        hasProperty('genomeReleaseId', equalTo(testData.regionPlatform.genomeReleaseId)),
                )),
        )
        assertThat regionRows[1], allOf(
                hasSameInterfaceProperties(Region, testData.regions[0], ['platform']),
                hasProperty('label', equalTo(testData.regions[0].name)),
                hasProperty('bioMarker', equalTo(testData.regions[0].geneSymbol)),
                hasProperty('platform', allOf(
                        hasProperty('id', equalTo(testData.regionPlatform.id)),
                        hasProperty('title', equalTo(testData.regionPlatform.title)),
                        hasProperty('organism', equalTo(testData.regionPlatform.organism)),
                        hasProperty('annotationDate', equalTo(testData.regionPlatform.annotationDate)),
                        hasProperty('markerType', equalTo(testData.regionPlatform.markerType)),
                        hasProperty('genomeReleaseId', equalTo(testData.regionPlatform.genomeReleaseId)),
                )),
        )

        assertThat regionRows[1][assayColumns[1]],
                hasSameInterfaceProperties(RnaSeqValues, testData.rnaseqData[0])
        assertThat regionRows[1][assayColumns[0]],
                hasSameInterfaceProperties(RnaSeqValues, testData.rnaseqData[1])
        assertThat regionRows[0][assayColumns[1]],
                hasSameInterfaceProperties(RnaSeqValues, testData.rnaseqData[2])
        assertThat regionRows[0][assayColumns[0]],
                hasSameInterfaceProperties(RnaSeqValues, testData.rnaseqData[3])

        assertThat(regionRows[1]*.normalizedReadcount,
                contains(testData.rnaseqData[-3..-4]*.normalizedReadcount.collect { Double it -> closeTo it, DELTA })
        )
        assertThat(regionRows[0]*.normalizedReadcount,
                contains(testData.rnaseqData[-1..-2]*.normalizedReadcount.collect { Double it -> closeTo it, DELTA })
        )
    }

    @Test
    void testLogIntensityProjection() {
        def logIntensityProjection = rnaseqResource.createProjection(
                [:], Projection.LOG_INTENSITY_PROJECTION)

        dataQueryResult = rnaseqResource.retrieveData(
                [trialNameConstraint], [], logIntensityProjection)

        def resultList = Lists.newArrayList(dataQueryResult)

        assertThat resultList, containsInAnyOrder(
                testData.regions.collect {
                    getDataMatcherForRegion(it, 'logNormalizedReadcount')
                })
    }


    @Test
    void testDefaultRealProjection() {

        def realProjection = rnaseqResource.createProjection(
                [:], Projection.DEFAULT_REAL_PROJECTION)

        dataQueryResult = rnaseqResource.retrieveData(
                [trialNameConstraint], [], realProjection)

        def resultList = Lists.newArrayList(dataQueryResult)

        assertThat resultList, containsInAnyOrder(
                testData.regions.collect {
                    getDataMatcherForRegion(it, 'normalizedReadcount')
                })
    }

    @Test
    void testZscoreProjection() {

        def zscoreProjection = rnaseqResource.createProjection(
                [:], Projection.ZSCORE_PROJECTION)

        dataQueryResult = rnaseqResource.retrieveData(
                [trialNameConstraint], [], zscoreProjection)

        def resultList = Lists.newArrayList(dataQueryResult)

        assertThat resultList, containsInAnyOrder(
                testData.regions.collect {
                    getDataMatcherForRegion(it, 'zscore')
                })
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
                Region, testData.regions[0], ['platform'])
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
                                        [chromosome: '1', start: 44, end: 8888],
                                        /* segment aligned at the end of test region;
                                         *segment shorter than region */
                                        [chromosome: '2', start: 88, end: 99],
                                ]
                        ]
                )
        ]

        def anotherPlatform = new DeGplInfo(
                title: 'Another Test Region Platform',
                organism: 'Homo Sapiens',
                annotationDate: Date.parse('yyyy-MM-dd', '2013-08-03'),
                markerType: 'RNASEQ_RCNT',
                genomeReleaseId: 'hg19',
        )
        anotherPlatform.id = 'test-another-platform'
        anotherPlatform.save failOnError: true, flush: true

        // this region should not appear in the result set
        def anotherRegion = new DeChromosomalRegion(
                platform: anotherPlatform,
                chromosome: '1',
                start: 1,
                end: 10,
                numberOfProbes: 42,
                name: 'region 1:1-10'
        )
        anotherRegion.id = -2010L
        anotherRegion.save failOnError: true, flush: true

        dataQueryResult = rnaseqResource.retrieveData assayConstraints, dataConstraints, projection

        def regionRows = Lists.newArrayList(dataQueryResult.rows)

        assertThat regionRows, hasSize(2)
        assertThat regionRows, contains(
                hasSameInterfaceProperties(
                        Region, testData.regions[1], ['platform']),
                hasSameInterfaceProperties(
                        Region, testData.regions[0], ['platform']))
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
                                        [chromosome: 'X'],
                                        [chromosome: '1', start: 1, end: 32],
                                        [chromosome: '2', start: 100, end: 1000],
                                ]
                        ]
                )
        ]

        dataQueryResult = rnaseqResource.retrieveData assayConstraints, dataConstraints, projection

        assertThat dataQueryResult, hasProperty('indicesList', is(not(empty())))
        assertThat Lists.newArrayList(dataQueryResult.rows), is(empty())
    }


    @Test
    void testWithGeneConstraint() {
        def assayConstraints = [
                rnaseqResource.createAssayConstraint(
                        AssayConstraint.TRIAL_NAME_CONSTRAINT, name: TRIAL_NAME),
                rnaseqResource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: testData.allPatientsQueryResult.id),
        ]
        def dataConstraints = [
                rnaseqResource.createDataConstraint([keyword_ids: [testData.searchKeywords.
                                                                           find({ it.keyword == 'AURKA' }).id]],
                        DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT
                )
        ]
        def projection = rnaseqResource.createProjection([:], RNASEQ_VALUES_PROJECTION)

        dataQueryResult = rnaseqResource.retrieveData(
                assayConstraints, dataConstraints, projection)

        def resultList = Lists.newArrayList dataQueryResult

        assertThat resultList, allOf(
                hasSize(1),
                everyItem(hasProperty('data', hasSize(2))),
                contains(hasProperty('bioMarker', equalTo('AURKA')))
        )
    }


    def getDataMatcherForRegion(DeChromosomalRegion region,
                                String property) {
        contains testData.rnaseqData.
                findAll { it.region == region }.
                sort { it.assay.id }. // data is sorted by assay id
                collect { closeTo it."$property" as Double, DELTA }
    }

    @Test
    void testAnnotationSearchGeneSymbol() {
        def gene_symbols = rnaseqResource.searchAnnotation(concept_code, 'AD', 'geneSymbol')
        assertThat gene_symbols, allOf(
                hasSize(1),
                contains(
                        equalTo("ADIRF")
                )
        )
    }

    @Test
    void testAnnotationSearchCytoband() {
        def cytoband = rnaseqResource.searchAnnotation(concept_code, '1p', 'cytoband')
        assertThat cytoband, allOf(
                hasSize(1),
                contains(
                        equalTo("1p12.1")
                )
        )
    }

    @Test
    void testAnnotationSearchName() {
        def names = rnaseqResource.searchAnnotation ( concept_code, 'region', 'name' )
        assertThat names, allOf (
                hasSize ( 2 ),
                // should be in alphabetical order
                contains (
                        equalTo ( "region 1:33-9999" ),
                        equalTo ( "region 2:66-99" )
                )
        )
    }

    @Test
    void testAnnotationSearchEmpty() {
        def symbols = rnaseqResource.searchAnnotation(concept_code, 'FOO', 'geneSymbol')
        assertThat symbols, hasSize(0)
    }

    @Test
    void testAnnotationSearchInvalid() {
        GroovyAssert.shouldFail(InvalidArgumentsException.class) {rnaseqResource.searchAnnotation(concept_code, 'T', 'FOO')}
    }

    @Test
    void testGeneSymbolAnnotationConstraint() {
        def constraint = new ConstraintByOmicsValue(
                omicsType: ConstraintByOmicsValue.OmicsType.RNASEQ_RCNT,
                property: 'geneSymbol',
                selector: 'AURKA',
                projectionType: Projection.LOG_NORMALIZED_READ_COUNT_PROJECTION
        )

        def distribution = rnaseqResource.getDistribution(constraint, conceptKey, null)
        def correctValues = testData.rnaseqData.findAll {it.region.geneSymbol == 'AURKA'}.collectEntries {[it.patient.id, it.logNormalizedReadcount]}
        assertThat distribution.size(), greaterThanOrEqualTo(1)
        assert distribution.equals(correctValues)
    }

    @Test
    void testCytobandAnnotationConstraint() {
        def constraint = new ConstraintByOmicsValue(
                omicsType: ConstraintByOmicsValue.OmicsType.RNASEQ_RCNT,
                property: 'cytoband',
                selector: '1p12.1',
                projectionType: Projection.LOG_NORMALIZED_READ_COUNT_PROJECTION
        )

        def distribution = rnaseqResource.getDistribution(constraint, conceptKey, null)
        def correctValues = testData.rnaseqData.findAll {it.region.cytoband == '1p12.1'}.collectEntries {[it.patient.id, it.logNormalizedReadcount]}
        assertThat distribution.size(), greaterThanOrEqualTo(1)
        assert distribution.equals(correctValues)
    }

    @Test
    void testNameAnnotationConstraint() {
        def constraint = new ConstraintByOmicsValue(
                omicsType: ConstraintByOmicsValue.OmicsType.RNASEQ_RCNT,
                property: 'name',
                selector: 'region 1:33-9999',
                projectionType: Projection.LOG_NORMALIZED_READ_COUNT_PROJECTION
        )

        def distribution = rnaseqResource.getDistribution(constraint, conceptKey, null)
        def correctValues = testData.rnaseqData.findAll {it.region.name == 'region 1:33-9999'}.collectEntries {[it.patient.id, it.logNormalizedReadcount]}
        assertThat distribution.size(), greaterThanOrEqualTo(1)
        assert distribution.equals(correctValues)
    }

    @Test
    void testLogNormalizedReadCountConstraint() {
        def constraint = new ConstraintByOmicsValue(
                omicsType: ConstraintByOmicsValue.OmicsType.RNASEQ_RCNT,
                property: 'name',
                selector: 'region 1:33-9999',
                projectionType: Projection.LOG_NORMALIZED_READ_COUNT_PROJECTION,
                operator: 'BETWEEN',
                constraint: '-0.5:0.5'
        )

        def distribution = rnaseqResource.getDistribution(constraint, conceptKey, null)
        def correctValues = testData.rnaseqData.findAll {it.region.name == 'region 1:33-9999' && -0.5 <= it.logNormalizedReadcount && it.logNormalizedReadcount <= 0.5}.collectEntries {[it.patient.id, it.logNormalizedReadcount]}
        assertThat distribution.size(), greaterThanOrEqualTo(1)
        assert distribution.equals(correctValues)
    }

    @Test
    void testNormalizedReadCountConstraint() {
        def constraint = new ConstraintByOmicsValue(
                omicsType: ConstraintByOmicsValue.OmicsType.RNASEQ_RCNT,
                property: 'name',
                selector: 'region 1:33-9999',
                projectionType: Projection.NORMALIZED_READ_COUNT_PROJECTION,
                operator: 'BETWEEN',
                constraint: '-0.5:1.5'
        )

        def distribution = rnaseqResource.getDistribution(constraint, conceptKey, null)
        def correctValues = testData.rnaseqData.findAll {it.region.name == 'region 1:33-9999' && -0.5 <= it.normalizedReadcount && it.normalizedReadcount <= 1.5}.collectEntries {[it.patient.id, it.normalizedReadcount]}
        assertThat distribution.size(), greaterThanOrEqualTo(1)
        assert distribution.equals(correctValues)
    }

    @Test
    void testZScoreConstraint() {
        def constraint = new ConstraintByOmicsValue(
                omicsType: ConstraintByOmicsValue.OmicsType.RNASEQ_RCNT,
                property: 'name',
                selector: 'region 1:33-9999',
                projectionType: Projection.ZSCORE_PROJECTION,
                operator: 'BETWEEN',
                constraint: '0.75:1.25'
        )

        def distribution = rnaseqResource.getDistribution(constraint, conceptKey, null)
        def correctValues = testData.rnaseqData.findAll {it.region.name == 'region 1:33-9999' && 0.75 <= it.zscore && it.zscore <= 1.25}.collectEntries {[it.patient.id, it.zscore]}
        assertThat distribution.size(), greaterThanOrEqualTo(1)
        assert distribution.equals(correctValues)
    }
}
