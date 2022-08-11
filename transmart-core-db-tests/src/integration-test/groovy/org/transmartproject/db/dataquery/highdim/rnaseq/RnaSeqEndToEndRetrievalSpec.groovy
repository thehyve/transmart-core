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
import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.springframework.beans.factory.annotation.Autowired
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
import org.transmartproject.db.TestData
import org.transmartproject.db.dataquery.highdim.DeGplInfo
import org.transmartproject.db.dataquery.highdim.chromoregion.DeChromosomalRegion
import spock.lang.Specification

import java.text.SimpleDateFormat

import static org.hamcrest.Matchers.*
import static org.transmartproject.db.dataquery.highdim.rnaseq.RnaSeqModule.RNASEQ_VALUES_PROJECTION
import static org.transmartproject.db.dataquery.highdim.rnaseq.RnaSeqTestData.TRIAL_NAME
import static org.transmartproject.db.test.Matchers.hasSameInterfaceProperties
import static spock.util.matcher.HamcrestSupport.that

@Integration
@Rollback
class RnaSeqEndToEndRetrievalSpec extends Specification {

    private static final double DELTA = 0.0001

    @Autowired
    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource<RegionRow> rnaseqResource

    TabularResult<AssayColumn, RegionRow> dataQueryResult

    Projection<RnaSeqValues> projection

    RnaSeqTestData testData

    AssayConstraint trialNameConstraint

    void setupData() {
        TestData.prepareCleanDatabase()

        testData = new RnaSeqTestData()
        testData.saveAll()

        rnaseqResource = highDimensionResourceService.getSubResourceForType 'rnaseq'

        trialNameConstraint = rnaseqResource.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: RnaSeqTestData.TRIAL_NAME)

        projection = rnaseqResource.createProjection([:], RNASEQ_VALUES_PROJECTION)
    }

    void cleanup() {
        dataQueryResult?.close()
    }

    void basicTest() {
        setupData()
        def assayConstraints = [
                rnaseqResource.createAssayConstraint(
                        AssayConstraint.TRIAL_NAME_CONSTRAINT, name: TRIAL_NAME),
                rnaseqResource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: testData.allPatientsQueryResult.queryInstances[0].queryResults[0].id),
        ]
        def dataConstraints = []

        dataQueryResult = rnaseqResource.retrieveData assayConstraints, dataConstraints, projection
        List<AssayColumn> assayColumns = dataQueryResult.indicesList
        Iterator<RegionRow> rows = dataQueryResult.rows
        def regionRows = Lists.newArrayList(rows)

        expect:
        that(dataQueryResult, allOf(
                is(notNullValue()),
                hasProperty('indicesList', contains(
                        /* they're ordered by assay id */
                        hasSameInterfaceProperties(Assay, testData.assays[1], ['platform']),
                        hasSameInterfaceProperties(Assay, testData.assays[0], ['platform']),
                )),
                hasProperty('rowsDimensionLabel', equalTo('Regions')),
                hasProperty('columnsDimensionLabel', equalTo('Sample codes')),
        ))
        regionRows.size() == 2
        /* results are ordered (asc) by region id */
        that(regionRows[0], allOf(
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
        ))
        that(regionRows[1], allOf(
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
        ))

        that(regionRows[1][assayColumns[1]],
                hasSameInterfaceProperties(RnaSeqValues, testData.rnaseqData[0]))
        that(regionRows[1][assayColumns[0]],
                hasSameInterfaceProperties(RnaSeqValues, testData.rnaseqData[1]))
        that(regionRows[0][assayColumns[1]],
                hasSameInterfaceProperties(RnaSeqValues, testData.rnaseqData[2]))
        that(regionRows[0][assayColumns[0]],
                hasSameInterfaceProperties(RnaSeqValues, testData.rnaseqData[3]))

        that(regionRows[1]*.normalizedReadcount,
                contains(testData.rnaseqData[-3..-4]*.normalizedReadcount.collect { Double it -> closeTo it, DELTA }))
        that(regionRows[0]*.normalizedReadcount,
                contains(testData.rnaseqData[-1..-2]*.normalizedReadcount.collect { Double it -> closeTo it, DELTA }))
    }

    void testLogIntensityProjection() {
        setupData()
        def logIntensityProjection = rnaseqResource.createProjection(
                [:], Projection.LOG_INTENSITY_PROJECTION)

        dataQueryResult = rnaseqResource.retrieveData(
                [trialNameConstraint], [], logIntensityProjection)

        def resultList = Lists.newArrayList(dataQueryResult)

        expect:
        that(resultList, containsInAnyOrder(
                testData.regions.collect {
                    getDataMatcherForRegion(it, 'logNormalizedReadcount')
                }))
    }


    void testDefaultRealProjection() {
        setupData()

        def realProjection = rnaseqResource.createProjection(
                [:], Projection.DEFAULT_REAL_PROJECTION)

        dataQueryResult = rnaseqResource.retrieveData(
                [trialNameConstraint], [], realProjection)

        def resultList = Lists.newArrayList(dataQueryResult)

        expect:
        that(resultList, containsInAnyOrder(
                testData.regions.collect {
                    getDataMatcherForRegion(it, 'normalizedReadcount')
                }))
    }

    void testZscoreProjection() {
        setupData()

        def zscoreProjection = rnaseqResource.createProjection(
                [:], Projection.ZSCORE_PROJECTION)

        dataQueryResult = rnaseqResource.retrieveData(
                [trialNameConstraint], [], zscoreProjection)

        def resultList = Lists.newArrayList(dataQueryResult)

        expect:
        that(resultList, containsInAnyOrder(
                testData.regions.collect {
                    getDataMatcherForRegion(it, 'zscore')
                }))
    }

    void testSegments_meetOne() {
        setupData()
        def assayConstraints = [
                rnaseqResource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: testData.allPatientsQueryResult.queryInstances[0].queryResults[0].id),
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

        expect:
        regionRows.size() == 1
        that(regionRows[0], hasSameInterfaceProperties(
                Region, testData.regions[0], ['platform']))
    }

    void testSegments_meetBoth() {
        setupData()
        def assayConstraints = [
                rnaseqResource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: testData.allPatientsQueryResult.queryInstances[0].queryResults[0].id),
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
                annotationDate: new SimpleDateFormat('yyyy-MM-dd').parse('2013-08-03'),
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

        expect:
        regionRows.size() == 2
        that(regionRows, contains(
                hasSameInterfaceProperties(
                        Region, testData.regions[1], ['platform']),
                hasSameInterfaceProperties(
                        Region, testData.regions[0], ['platform'])))
    }

    void testSegments_meetNone() {
        setupData()
        def assayConstraints = [
                rnaseqResource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: testData.allPatientsQueryResult.queryInstances[0].queryResults[0].id),
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

        expect:
        !dataQueryResult.indicesList.empty
        Lists.newArrayList(dataQueryResult.rows).empty
    }


    void testWithGeneConstraint() {
        setupData()
        def assayConstraints = [
                rnaseqResource.createAssayConstraint(
                        AssayConstraint.TRIAL_NAME_CONSTRAINT, name: TRIAL_NAME),
                rnaseqResource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: testData.allPatientsQueryResult.queryInstances[0].queryResults[0].id),
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

        expect:
        that(resultList, allOf(
                hasSize(1),
                everyItem(hasProperty('data', hasSize(2))),
                contains(hasProperty('bioMarker', equalTo('AURKA')))
        ))
    }


    def getDataMatcherForRegion(DeChromosomalRegion region,
                                String property) {
        contains testData.rnaseqData.
                findAll { it.region == region }.
                sort { it.assay.id }. // data is sorted by assay id
                collect { closeTo it."$property" as Double, DELTA }
    }
}
