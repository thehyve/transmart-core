/*
 * Copyright © 2013-2015 The Hyve B.V.
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

package org.transmartproject.db.dataquery.highdim.snp_lz

import com.google.common.collect.Lists
import grails.test.mixin.TestMixin
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.db.test.Matchers.hasSameInterfaceProperties

@TestMixin(RuleBasedIntegrationTestMixin)
class SnpLzEndToEndRetrievalTest {

    /* scale of the zscore column is 5, so this is the max rounding error */
    private static Double DELTA = 0.000005

    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource snpLzResource

    @Lazy AssayConstraint trialConstraint = snpLzResource.createAssayConstraint(
            AssayConstraint.TRIAL_NAME_CONSTRAINT,
            name: SnpLzTestData.TRIAL_NAME)

    @Lazy AssayConstraint conceptConstraint0 = snpLzResource.createAssayConstraint(
        AssayConstraint.ONTOLOGY_TERM_CONSTRAINT,
        concept_key: SnpLzTestData.CONCEPT_PATH)

    @Lazy Projection allDataProjection = snpLzResource.createProjection([:],
            Projection.ALL_DATA_PROJECTION)

    TabularResult<AssayColumn, SnpLzRow> result

    SnpLzTestData testData = new SnpLzTestData()

    @Before
    void setup() {
        testData.saveAll()
        snpLzResource = highDimensionResourceService.getSubResourceForType('snp_lz')
    }

    @After
    void tearDown() {
        result?.close()
    }

    @Test
    void fetchAllDataTestSizes() {
        result = snpLzResource.retrieveData(
                [trialConstraint, conceptConstraint0], [], allDataProjection)

        List rows = Lists.newArrayList result.rows

        assertThat rows, allOf(
                hasSize(testData.annotations.size()),
                everyItem(allOf(
                        isA(SnpLzRow),
                        contains(
                                isA(SnpLzAllDataCell),
                                isA(SnpLzAllDataCell),
                                isA(SnpLzAllDataCell)),
                )))
    }

    @Test
    void fetchAllDataTestRows() {
        result = snpLzResource.retrieveData(
                [trialConstraint, conceptConstraint0], [], allDataProjection)

        List rows = Lists.newArrayList result.rows

        def expectedGeneInfo = [
                rs28616230: 'ND1',
                rs1599988: 'ND1',
                rs199476129: 'ND2/COX1',
        ]

        // rows should be ordered by id
        // annotation properties
        assertThat rows, contains(
                testData.annotations.sort { it.id }.collect { GenotypeProbeAnnotation ann ->
                    allOf(
                            hasProperty('bioMarker', is(expectedGeneInfo[ann.snpName])),
                            hasProperty('label',  is(ann.snpName)),
                            hasProperty('snpName', is(ann.snpName)),
                    )
                }
        )

        // SnpDataByProbe properties
        assertThat rows, contains(
                testData.data
                        .findAll { it.bioAssayGenoPlatform.bioAssayPlatform.accession == SnpLzTestData.PLATFORM }
                        .sort { it.genotypeProbeAnnotation.id }
                        .collect { SnpDataByProbeCoreDb snpData ->
                            allOf(
                                    hasProperty('a1', is(snpData.a1)),
                                    hasProperty('a2', is(snpData.a2)),
                                    hasProperty('imputeQuality', closeTo(snpData.imputeQuality, DELTA)),
                                    hasProperty('GTProbabilityThreshold',
                                            closeTo(snpData.gtProbabilityThreshold, DELTA)),
                                    hasProperty('minorAlleleFrequency', closeTo(snpData.maf, DELTA)),
                                    hasProperty('minorAllele', is(snpData.minorAllele)),
                                            hasProperty('a1a1Count', is(snpData.getCA1A1() as Long)),
                                    hasProperty('a1a2Count', is(snpData.getCA1A2() as Long)),
                                    hasProperty('a2a2Count', is(snpData.getCA2A2() as Long)),
                                    hasProperty('noCallCount', is(snpData.getCNocall() as Long)),
                            )
                        }
        )
    }

    // for all data projection only
    private Matcher dataMatcherFor(annotations, assays) {
        def orderedSampleCodes = assays.findAll { it.platform.id == SnpLzTestData.PLATFORM }*.sampleCode

        def orderedSnpData = testData.data
                .findAll { it.bioAssayGenoPlatform.bioAssayPlatform.accession == SnpLzTestData.PLATFORM }
                .grep { it.genotypeProbeAnnotation.id in annotations*.id }
                .sort { it.genotypeProbeAnnotation.id }

        contains(
                orderedSnpData.collect { SnpDataByProbeCoreDb snpData ->
                    allOf(
                            hasProperty('snpName', is(snpData.genotypeProbeAnnotation.snpName)),
                            hasProperty('chromosome', is(snpData.genotypeProbeAnnotation.chromosome)),
                            hasProperty('position', is(snpData.genotypeProbeAnnotation.pos)),
                            contains(
                                    orderedSampleCodes.collect { sampleCode ->
                                        def gps = testData.sampleGps
                                                .get(sampleCode, snpData.genotypeProbeAnnotation.snpName)
                                                .split(' ')
                                        def gts = testData.sampleGts
                                                .get(sampleCode, snpData.genotypeProbeAnnotation.snpName)
                                                .split(' ')
                                        def doses = testData.sampleDoses
                                                .get(sampleCode, snpData.genotypeProbeAnnotation.snpName)

                                        allOf(
                                                hasProperty('probabilityA1A1', closeTo(gps[0] as Double, DELTA)),
                                                hasProperty('probabilityA1A2', closeTo(gps[1] as Double, DELTA)),
                                                hasProperty('probabilityA2A2', closeTo(gps[2] as Double, DELTA)),

                                                hasProperty('likelyGenotype', is("${gts[0]}_${gts[1]}" as String)),

                                                hasProperty('minorAlleleDose', closeTo(doses as Double, DELTA)),
                                        )
                                    }
                            ))
                }
        )
    }

    @Test
    void fetchAllDataTestCells() {
        result = snpLzResource.retrieveData(
                [trialConstraint, conceptConstraint0], [], allDataProjection)

        List rows = Lists.newArrayList result.rows

        /* Test correspondence between testData.assays and result.indicesList:
         * - test if the collection of assay ids returning from the query is
         *   the same as in the test data and that the assays are ordered by
         *   assay id.
         * - test if the subject ids (sampleCode) are also available in the
         *   retrieved assay data.
         */
        assert result.indicesList*.id == testData.orderedAssaysByPlatform[SnpLzTestData.PLATFORM]*.id
        assert result.indicesList*.sampleCode == testData.orderedAssaysByPlatform[SnpLzTestData.PLATFORM]*.sampleCode
        // test correspondence between testData and result rows
        assertThat rows, dataMatcherFor(testData.annotations, testData.orderedAssays)
    }

    @Test
    void retrieveAssaysEqualsIndicesList() {
        result = snpLzResource.retrieveData(
                [trialConstraint, conceptConstraint0], [], allDataProjection)
        /*
         * Test if the list of assay ids returning from the query is
         * the same as in the test data and that the assays are ordered by
         * assay id.
         */
        assert result.indicesList*.id == testData.orderedAssaysByPlatform[SnpLzTestData.PLATFORM]*.id

        def assays = snpLzResource.retrieveAssays([trialConstraint, conceptConstraint0])
        /*
         * Test if retrieveAssays returns the same list as indicesList in the
         * TabularResult.
         */
        assert result.indicesList == assays
    }

    @Test
    void testIndexingByNumber() {
        result = snpLzResource.retrieveData(
                [trialConstraint, conceptConstraint0], [], allDataProjection)

        List rows = Lists.newArrayList result.rows

        // test correspondence between testData and result rows
        assertThat rows, dataMatcherFor(testData.annotations, testData.orderedAssaysByPlatform[SnpLzTestData.PLATFORM])
        SnpLzRow firstLzRow = rows.first()

        assert firstLzRow[1] == Lists.newArrayList(firstLzRow.iterator())[1]
    }

    @Test
    void testIndexingByAssay() {
        result = snpLzResource.retrieveData(
                [trialConstraint, conceptConstraint0], [], allDataProjection)

        List rows = Lists.newArrayList result.rows

        // test correspondence between testData and result rows
        assertThat rows, dataMatcherFor(testData.annotations, testData.orderedAssaysByPlatform[SnpLzTestData.PLATFORM])
        SnpLzRow firstLzRow = rows.first()

        assert firstLzRow[result.indicesList[1]] ==
                Lists.newArrayList(firstLzRow.iterator())[1]
    }

    @Test
    void fetchFilterBySample() {
        def selectedAssay = testData.assays[1]
        def assayConstraint = snpLzResource.createAssayConstraint(
                AssayConstraint.ASSAY_ID_LIST_CONSTRAINT,
                ids: [selectedAssay.id])
        result = snpLzResource.retrieveData(
                [trialConstraint, conceptConstraint0, assayConstraint], [], allDataProjection)

        assertThat result.indicesList,
                contains(hasSameInterfaceProperties(Assay, selectedAssay))

        List rows = Lists.newArrayList result.rows

        assertThat rows, dataMatcherFor(testData.annotations, [selectedAssay])
    }

    @Test
    void fetchFilterDataBySnpName() {
        def selectedAnnotation = testData.annotations[2]
        def selectedSnpName = selectedAnnotation.snpName

        def dataConstraint = snpLzResource.createDataConstraint(
                'snps', names: [selectedSnpName])
        result = snpLzResource.retrieveData(
                [trialConstraint, conceptConstraint0], [dataConstraint], allDataProjection)

        List rows = Lists.newArrayList result.rows

        // test correspondence between testData and result rows
        assertThat rows, dataMatcherFor([selectedAnnotation], testData.orderedAssaysByPlatform[SnpLzTestData.PLATFORM])
    }

    @Test
    void fetchFilterDataByGene() {
        def gene = 'ND2'
        def selectedAnnotation =
                testData.annotations.find { it.geneInfo.contains(gene) }

        def dataConstraint = snpLzResource.createDataConstraint(
                DataConstraint.GENES_CONSTRAINT, names: [gene])
        result = snpLzResource.retrieveData(
                [trialConstraint, conceptConstraint0], [dataConstraint], allDataProjection)

        List rows = Lists.newArrayList result.rows

        // test correspondence between testData and result rows
        assertThat rows, dataMatcherFor([selectedAnnotation], testData.orderedAssaysByPlatform[SnpLzTestData.PLATFORM])
    }

    @Test
    void fetchFilterByChromosomeLocation() {
        def locationConstraint = snpLzResource.createDataConstraint(
                DataConstraint.CHROMOSOME_SEGMENT_CONSTRAINT,
                chromosome: '1', start: 4100, end: 4250)
        result = snpLzResource.retrieveData(
                [trialConstraint, conceptConstraint0], [locationConstraint], allDataProjection)

        List rows = Lists.newArrayList result.rows

        assertThat rows, hasSize(2)
        // test correspondence between testData and result rows
        assertThat rows, dataMatcherFor(testData.annotations[0..1], testData.orderedAssaysByPlatform[SnpLzTestData.PLATFORM])
    }

    @Test
    void testFindsAssays() {
        def assayConstraint = highDimensionResourceService.
                createAssayConstraint(AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: SnpLzTestData.TRIAL_NAME)

        def res = highDimensionResourceService.
                getSubResourcesAssayMultiMap([assayConstraint])

        assertThat res, hasEntry(
                hasProperty('dataTypeName', is('snp_lz')),
                containsInAnyOrder(
                        testData.assays.collect {
                            hasSameInterfaceProperties(Assay, it)
                        }
                )
        )
    }

    private List otherProjectionsCommon(String projectionName) {
        def projection = snpLzResource.createProjection(projectionName)
        def assay = testData.assays[0]
        def assaySampleCode = assay.sampleCode
        def rsId = testData.annotations[0].snpName

        def assayConstraint = snpLzResource.createAssayConstraint(
                AssayConstraint.ASSAY_ID_LIST_CONSTRAINT,
                ids: [assay.id])

        result = snpLzResource.retrieveData(
                [assayConstraint], [], projection)

        List rows = Lists.newArrayList result.rows

        assertThat rows, hasItem(hasProperty('snpName', is(rsId)))

        def relevantRow = rows.find { it.snpName == rsId }
        assert relevantRow != null

        [relevantRow, assaySampleCode, rsId]
    }

    @Test
    void testAllelesProjection() {
        def (relevantRow, assaySampleCode, rsId) =
                otherProjectionsCommon('alleles')

        assertThat relevantRow, contains(
                is(testData.sampleGts.get(assaySampleCode, rsId).replace(' ', '_'))
        )
    }

    @Test
    void testProbabilitiesProjection() {
        def (relevantRow, assaySampleCode, rsId) =
                otherProjectionsCommon('probabilities')

        assertThat relevantRow, contains(
                is(testData.sampleGps.get(assaySampleCode, rsId).split(' ').collect { it as double }
                        as SnpLzProbabilitiesCell)
        )
    }

    @Test
    void testDoseProjection() {
        def (relevantRow, assaySampleCode, rsId) =
        otherProjectionsCommon('dose')

        assertThat relevantRow, contains(
                is(testData.sampleDoses.get(assaySampleCode, rsId) as double)
        )
    }
}
