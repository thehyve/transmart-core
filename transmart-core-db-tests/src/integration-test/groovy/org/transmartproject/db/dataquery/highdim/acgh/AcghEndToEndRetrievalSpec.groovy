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

package org.transmartproject.db.dataquery.highdim.acgh

import com.google.common.collect.Lists
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.acgh.AcghValues
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.chromoregion.Region
import org.transmartproject.core.dataquery.highdim.chromoregion.RegionRow
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.TestData
import org.transmartproject.db.dataquery.highdim.DeGplInfo
import org.transmartproject.db.dataquery.highdim.chromoregion.DeChromosomalRegion
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static org.transmartproject.db.dataquery.highdim.acgh.AcghModule.ACGH_VALUES_PROJECTION
import static org.transmartproject.db.dataquery.highdim.acgh.AcghTestData.TRIAL_NAME
import static org.transmartproject.db.test.Matchers.hasSameInterfaceProperties
import static spock.util.matcher.HamcrestSupport.that

@Integration
@Rollback
class AcghEndToEndRetrievalSpec extends Specification {

    @Autowired
    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource<RegionRow> acghResource

    TabularResult<AssayColumn, RegionRow> dataQueryResult

    Projection<AcghValues> projection

    AcghTestData testData

    void setupData() {
        TestData.clearAllData()

        testData = new AcghTestData()
        testData.saveAll()

        acghResource = highDimensionResourceService.getSubResourceForType 'acgh'

        /* projection never varies in our tests */
        projection = acghResource.createProjection([:], ACGH_VALUES_PROJECTION)
    }

    void cleanup() {
        dataQueryResult?.close()
    }

    void testBasic() {
        setupData()
        def assayConstraints = [
                acghResource.createAssayConstraint(
                        AssayConstraint.TRIAL_NAME_CONSTRAINT, name: TRIAL_NAME),
                acghResource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: testData.allPatientsQueryResult.queryInstances[0].queryResults[0].id),
        ]
        def dataConstraints = []

        when:
        dataQueryResult = acghResource.retrieveData assayConstraints, dataConstraints, projection

        then:
        dataQueryResult.rowsDimensionLabel == 'Regions'
        dataQueryResult.columnsDimensionLabel == 'Sample codes'
        that(dataQueryResult.indicesList, contains(
                /* they're ordered by assay id */
                hasSameInterfaceProperties(Assay, testData.assays[1], ['platform']),
                hasSameInterfaceProperties(Assay, testData.assays[0], ['platform']),
        ))

        when:
        List<AssayColumn> assayColumns = dataQueryResult.indicesList

        Iterator<RegionRow> rows = dataQueryResult.rows
        def regionRows = Lists.newArrayList(rows)

        then:
        regionRows hasSize(2)
        /* results are ordered (asc) by region id */
        regionRows[0] allOf(
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

        regionRows[1] allOf(
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

        regionRows[1][assayColumns[1]]
        hasSameInterfaceProperties(AcghValues, testData.acghData[0])
        regionRows[1][assayColumns[0]]
        hasSameInterfaceProperties(AcghValues, testData.acghData[1])
        regionRows[0][assayColumns[1]]
        hasSameInterfaceProperties(AcghValues, testData.acghData[2])
        regionRows[0][assayColumns[0]]
        hasSameInterfaceProperties(AcghValues, testData.acghData[3])
    }

    void testSegments_meetOne() {
        setupData()
        def assayConstraints = [
                acghResource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: testData.allPatientsQueryResult.queryInstances[0].queryResults[0].id),
        ]
        def dataConstraints = [
                // start matches start of regions[0]
                acghResource.createDataConstraint(
                        DataConstraint.CHROMOSOME_SEGMENT_CONSTRAINT,
                        chromosome: '1', start: 33, end: 44
                )
        ]

        dataQueryResult = acghResource.retrieveData assayConstraints, dataConstraints, projection

        def regionRows = Lists.newArrayList(dataQueryResult.rows)

        expect:
        regionRows hasSize(1)
        regionRows[0] hasSameInterfaceProperties(
                Region, testData.regions[0], ['platform'])
    }

    void testSegments_meetBoth() {
        setupData()
        def assayConstraints = [
                acghResource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: testData.allPatientsQueryResult.queryInstances[0].queryResults[0].id),
        ]
        def dataConstraints = [
                // start matches start of regions[0]
                acghResource.createDataConstraint(
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
                markerType: 'Chromosomal',
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
        anotherRegion.id = -2000L
        anotherRegion.save failOnError: true, flush: true

        dataQueryResult = acghResource.retrieveData assayConstraints, dataConstraints, projection

        def regionRows = Lists.newArrayList(dataQueryResult.rows)

        expect:
        regionRows hasSize(2)
        regionRows contains(
                hasSameInterfaceProperties(
                        Region, testData.regions[1], ['platform']),
                hasSameInterfaceProperties(
                        Region, testData.regions[0], ['platform']))
    }

    void testSegments_meetNone() {
        setupData()
        def assayConstraints = [
                acghResource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: testData.allPatientsQueryResult.queryInstances[0].queryResults[0].id),
        ]
        def dataConstraints = [
                // start matches start of regions[0]
                acghResource.createDataConstraint(
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

        when:
        dataQueryResult = acghResource.retrieveData assayConstraints, dataConstraints, projection

        then:
        !dataQueryResult.indicesList.empty
        !dataQueryResult.rows.hasNext()
    }

    void testResultRowsAreCoreApiRegionRows() {
        setupData()
        def assayConstraints = [
                acghResource.createAssayConstraint(
                        AssayConstraint.TRIAL_NAME_CONSTRAINT, name: TRIAL_NAME)]

        when:
        dataQueryResult = acghResource.retrieveData assayConstraints, [], projection

        then:
        dataQueryResult.every { it instanceof RegionRow }
    }

    void testWithGeneConstraint() {
        setupData()
        def assayConstraints = [
                acghResource.createAssayConstraint(
                        AssayConstraint.TRIAL_NAME_CONSTRAINT, name: TRIAL_NAME),
                acghResource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: testData.allPatientsQueryResult.queryInstances[0].queryResults[0].id),
        ]
        def dataConstraints = [
                acghResource.createDataConstraint([keyword_ids: [testData.searchKeywords.
                                                                         find({ it.keyword == 'AURKA' }).id]],
                        DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT
                )
        ]
        def projection = acghResource.createProjection([:], ACGH_VALUES_PROJECTION)

        dataQueryResult = acghResource.retrieveData(
                assayConstraints, dataConstraints, projection)

        List resultList = Lists.newArrayList dataQueryResult

        expect:
        resultList.size() == 1
        resultList.every { it.data.size() == 2 }
        resultList.find { it.bioMarker == 'AURKA' }
    }

}
