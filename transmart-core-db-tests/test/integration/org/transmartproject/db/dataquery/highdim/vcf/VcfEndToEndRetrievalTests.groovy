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

package org.transmartproject.db.dataquery.highdim.vcf

import com.google.common.collect.Lists
import grails.test.mixin.TestMixin
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.db.TestDataHelper
import org.transmartproject.db.test.Matchers
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Created by j.hudecek on 17-3-14.
 */
@TestMixin(RuleBasedIntegrationTestMixin)
class VcfEndToEndRetrievalTests {

    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource vcfResource

    Closeable dataQueryResult

    VcfTestData testData = new VcfTestData()

    AssayConstraint trialNameConstraint

    @Before
    void setUp() {
        testData.saveAll()

        vcfResource = highDimensionResourceService.getSubResourceForType 'vcf'
        assertThat vcfResource, is(notNullValue())

        trialNameConstraint = vcfResource.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: VcfTestData.TRIAL_NAME)
    }

    @After
    void after() {
        dataQueryResult?.close()
    }

    @Test
    void basicTestWithConstraints() {
        List dataConstraints = [vcfResource.createDataConstraint(
                DataConstraint.DISJUNCTION_CONSTRAINT,
                subconstraints: [
                        (DataConstraint.CHROMOSOME_SEGMENT_CONSTRAINT): [chromosome: "1", start: 1, end: 2]
                ]
        )]
        def projection = vcfResource.createProjection [:], 'cohort'

        dataQueryResult = vcfResource.retrieveData(
                [], dataConstraints, projection)

        def resultList = Lists.newArrayList(dataQueryResult)

        assertThat resultList, hasSize(2)

        assertThat resultList, everyItem(
                Matchers.hasEqualValueProperties('position', 'quality')
        )
        assertThat resultList, hasItem(
                allOf(
                        hasProperty('position', equalTo(1L)),
                        hasProperty('referenceAllele', equalTo('C')),
                        hasProperty('alternatives', equalTo('A'))
                )
        )
        assertThat resultList, hasItem(
                allOf(
                        hasProperty('position', equalTo(2L)),
                        hasProperty('referenceAllele', equalTo('GCCCCC')),
                        hasProperty('alternatives', equalTo('GCCCC'))
                )
        )
        assertThat resultList, not(hasItem(
                allOf(
                        hasProperty('position', equalTo(3L)),
                )
        ))
    }

    @Test
    void testAssayFilter() {
        List dataConstraints = [vcfResource.createDataConstraint(
                DataConstraint.DISJUNCTION_CONSTRAINT,
                subconstraints: [
                        (DataConstraint.CHROMOSOME_SEGMENT_CONSTRAINT): [chromosome: "1", start: 1, end: 2]
                ]
        )]
        def projection = vcfResource.createProjection [:], 'cohort'

        dataQueryResult = vcfResource.retrieveData(
                [], dataConstraints, projection)

        // Make sure that only the VCF assays are returned
        assertThat dataQueryResult.indicesList, hasSize(3)

        assertThat dataQueryResult.indicesList, everyItem(
                hasProperty('platform',
                        hasProperty('markerType', equalTo('VCF'))
        ))
    }
    
    @Test
    void testVcfDataRowRetrieval() {
        List dataConstraints = []
        def projection = vcfResource.createProjection [:], 'cohort'

        dataQueryResult = vcfResource.retrieveData(
                [], dataConstraints, projection)

        def resultList = []
        for (VcfDataRow region : dataQueryResult.rows) {
            resultList.add(region)
        }

        /*
        chr: 1,
        pos: position,
        rsId: '.',
        ref: reference,
        alt: alternative,
        quality: position/2, //nonsensical value
        filter: '.',
        info:  info,
        format: ''
            createDetail(1, 'C', 'A', 'DP=88;AF1=1;QD=2;DP4=0,0,80,0;MQ=60;FQ=-268'),
            createDetail(2, 'GCCCCC', 'GCCCC', 'DP=88;AF1=1;QD=2;DP4=0,0,80,0;MQ=60;FQ=-268'),
            createDetail(3, 'A', 'C,T', 'DP=88;AF1=1;QD=2;DP4=0,0,80,0;MQ=60;FQ=-268')
        */
        //{assayId=-1403, allele2=2, allele1=1, format=, variant=null, reference=true, variantType=SNV,
        // info=DP=88;AF1=1;QD=2;DP4=0,0,80,0;MQ=60;FQ=-268, pos=1, ref=C, rsId=., alt=A, subjectId=SAMPLE_FOR_-803,
        // quality=1, variantFormat=R/R, filter=., chr=1}
        assertThat resultList, hasItem(
                allOf(
                        hasProperty('datasetId', equalTo("BOGUSDTST")),
                        hasProperty('chromosome', equalTo("1")),
                        hasProperty('position', equalTo(1L)),
                        hasProperty('rsId', equalTo(".")),

                        hasProperty('referenceAllele', equalTo("C")),
                        hasProperty('alternatives', equalTo("A")),

                        hasProperty('quality', equalTo("1")),
                        hasProperty('filter', equalTo(".")),
                        hasProperty('info', equalTo("DP=88;AF1=1;QD=2;DP4=0,0,80,0;MQ=60;FQ=-268")),
                        hasProperty('format', equalTo("GT")),
                        hasProperty('variants', equalTo("1/1\t2/2\t2/2")),

                        hasProperty('qualityOfDepth', closeTo( 2.0 as Double, 0.01 as Double )),
                        hasProperty('formatFields', hasItem( equalTo( "GT" ) ) ),

                        hasProperty('infoFields', allOf(
                                hasEntry(equalTo('DP'),equalTo('88')),
                                hasEntry(equalTo('AF1'),equalTo('1')),
                                hasEntry(equalTo('QD'),equalTo('2')),
                                hasEntry(equalTo('DP4'),equalTo('0,0,80,0')),
                                hasEntry(equalTo('MQ'),equalTo('60')),
                                hasEntry(equalTo('FQ'),equalTo('-268')),

                        ))
                )
        )
        assertThat resultList, hasItem(
                allOf(
                    hasProperty('position', equalTo(2L)),

                    hasProperty('referenceAllele', equalTo("GCCCCC")),
                    hasProperty('alternatives', equalTo("GCCCC")),

                )
        )
        assertThat resultList, hasItem(
                allOf(
                        hasProperty('position', equalTo(3L)),
                        hasProperty('referenceAllele', equalTo("A")),
                        hasProperty('alternatives', equalTo("C,T")),

                )
        )
    }

    @Test
    void testCohortProjection() {
        List dataConstraints = []
        def projection = vcfResource.createProjection [:], 'cohort'

        dataQueryResult = vcfResource.retrieveData(
                [], dataConstraints, projection)

        def resultList = []
        for (VcfDataRow row: dataQueryResult.rows) {
            resultList.add(row)
        }

        // Please note: the order of the assays is opposite from the order of creation
        // as the assayId is decreased while creating the assays
        assertThat resultList, hasItem(
                contains(
                        allOf(
                                hasEntry(equalTo('allele1'),equalTo(1)),
                                hasEntry(equalTo('allele2'),equalTo(1)),
                                hasEntry(equalTo('subjectId'),equalTo("SAMPLE_FOR_-803")),
                                hasEntry(equalTo('subjectPosition'),equalTo(3L))
                        ),
                        allOf(
                                hasEntry(equalTo('allele1'),equalTo(0)),
                                hasEntry(equalTo('allele2'),equalTo(1)),
                                hasEntry(equalTo('subjectId'),equalTo("SAMPLE_FOR_-802")),
                                hasEntry(equalTo('subjectPosition'),equalTo(2L))
                        ),
                        allOf(
                                hasEntry(equalTo('allele1'),equalTo(1)),
                                hasEntry(equalTo('allele2'),equalTo(0)),
                                hasEntry(equalTo('subjectId'),equalTo("SAMPLE_FOR_-801")),
                                hasEntry(equalTo('subjectPosition'),equalTo(1L))
                        ),
                )
        )
    }

    @Test
    void testVariantProjection() {
        List dataConstraints = []
        def projection = vcfResource.createProjection [:], 'variant'

        dataQueryResult = vcfResource.retrieveData(
                [], dataConstraints, projection)

        def resultList = []
        for (VcfDataRow row: dataQueryResult.rows) {
            resultList.add(row)
        }

        def expected
        def indices = dataQueryResult.indicesList
        def assayOrder = [ indices[ ]]

        assertThat resultList, hasItem( allOf(
                hasProperty('referenceAllele', equalTo( 'C' ) ),
                hasProperty('alternatives', equalTo( 'A' ) ),

                // Please note: the order of the assays is opposite from the order of creation
                // as the assayId is decreased while creating the assays
                contains( "A/A", "C/A", "A/C" )
        ))
    }

    @Test
    void testNonUniquePosChrEntries() {
        // Add other DeVariantSubjectDetailCoreDb with same chr, pos as existing
        def detail = testData.createDetail(3, 'A', 'G', 'DP=88;AF1=1;QD=2;DP4=0,0,80,0;MQ=60;FQ=-268')
        detail.rsId = 'dummyId'

        int mut = 0
        def summariesData = []
        testData.indexData.eachWithIndex { summaryIndex, idx ->
            mut++
            def summary = testData.createSummary(detail, mut & 1, (mut & 2) >> 1, testData.assays[idx], summaryIndex)
            summary.rsId = detail.rsId
            summariesData += summary
        }
        detail.save()
        TestDataHelper.save(summariesData)


        List dataConstraints = [vcfResource.createDataConstraint(
                DataConstraint.DISJUNCTION_CONSTRAINT,
                subconstraints: [
                        (DataConstraint.CHROMOSOME_SEGMENT_CONSTRAINT): [chromosome: "1", start: 3, end: 3]
                ]
        )]
        def projection = vcfResource.createProjection [:], 'cohort'

        dataQueryResult = vcfResource.retrieveData(
                [], dataConstraints, projection)

        def resultList = dataQueryResult.rows.toList()
        assertThat(resultList, hasSize(2))
        assertThat(resultList, hasItem(allOf(
                hasProperty('referenceAllele', equalTo('A')),
                hasProperty('alternatives', equalTo('C,T'))
        )))
        assertThat(resultList, hasItem(allOf(
                hasProperty('referenceAllele', equalTo('A')),
                hasProperty('alternatives', equalTo('G'))
        )))
    }

    @Test
    void testVcfPlatformIsRecognized() {
        def constraint = highDimensionResourceService.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: VcfTestData.TRIAL_NAME)

        def map = highDimensionResourceService.
                getSubResourcesAssayMultiMap([constraint])

        assertThat map, hasKey(
                hasProperty('dataTypeName', equalTo('vcf')))

        def entry = map.entrySet().find { it.key.dataTypeName == 'vcf' }

        assertThat entry.value, allOf(
                hasSize(greaterThan(0)),
                everyItem(
                        hasProperty('platform',
                                hasProperty('markerType',
                                        equalTo('VCF')))))
    }
    
    @Test
    void testOriginalSubjectData() {
        List dataConstraints = []
        def projection = vcfResource.createProjection [:], 'cohort'
        dataQueryResult = vcfResource.retrieveData(
                [], dataConstraints, projection)

        // Make sure that the original variant values are returned properly
        // Please note: the order of the assays is opposite from the order of creation
        // as the assayId is decreased while creating the assays
        def expected = [
            ["2/2", "2/2", "1/1"],
            ["4/4", "3/3", "2/2"],
            ["6/6", "4/4", "3/3"],
        ]
        
        def assays = dataQueryResult.indicesList
        def rows = dataQueryResult.getRows()
        
        expected.each { position ->
            def row = rows.next()
            
            position.eachWithIndex { result, assayIndex ->
                assertThat row.getOriginalSubjectData(assays[assayIndex]), equalTo(result)
            }
        }
    }
}
