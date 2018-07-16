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
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import spock.lang.Specification
import spock.lang.Ignore

import static org.hamcrest.Matchers.*
import static org.transmartproject.db.test.Matchers.hasSameInterfaceProperties
import static spock.util.matcher.HamcrestSupport.that

@Integration
@Rollback
class SnpLzEndToEndRetrievalSpec extends Specification {

    /* scale of the zscore column is 5, so this is the max rounding error */
    private static Double DELTA = 0.000005

    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource snpLzResource

    AssayConstraint trialConstraint

    AssayConstraint conceptConstraint0

    Projection allDataProjection

    TabularResult<AssayColumn, SnpLzRow> dataQueryResult

    SnpLzTestData testData = new SnpLzTestData()

    void setupData() {
        testData.saveAll()
        snpLzResource = highDimensionResourceService.getSubResourceForType('snp_lz')


        trialConstraint = snpLzResource.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: SnpLzTestData.TRIAL_NAME)

        conceptConstraint0 = snpLzResource.createAssayConstraint(
                AssayConstraint.ONTOLOGY_TERM_CONSTRAINT,
                concept_key: SnpLzTestData.CONCEPT_PATH)

        allDataProjection = snpLzResource.createProjection([:],
                Projection.ALL_DATA_PROJECTION)
    }

    void cleanup() {
        dataQueryResult?.close()
    }

    void testFetchAllDataTestSizes() {
        setupData()
        dataQueryResult = snpLzResource.retrieveData(
                [trialConstraint, conceptConstraint0], [], allDataProjection)

        List rows = Lists.newArrayList(dataQueryResult.rows)

        expect:
        that rows, allOf(
                hasSize(testData.annotations.size()),
                everyItem(allOf(
                        isA(SnpLzRow),
                        contains(
                                isA(SnpLzAllDataCell),
                                isA(SnpLzAllDataCell),
                                isA(SnpLzAllDataCell)),
                )))
    }

    void testFetchAllDataTestRows() {
        setupData()
        dataQueryResult = snpLzResource.retrieveData(
                [trialConstraint, conceptConstraint0], [], allDataProjection)

        List rows = Lists.newArrayList(dataQueryResult.rows)

        def expectedGeneInfo = [
                rs28616230 : 'ND1',
                rs1599988  : 'ND1',
                rs199476129: 'ND2/COX1',
        ]

        expect:
        // rows should be ordered by id
        // annotation properties
        that rows, contains(
                testData.annotations.sort { it.id }.collect { GenotypeProbeAnnotation ann ->
                    allOf(
                            hasProperty('bioMarker', is(expectedGeneInfo[ann.snpName])),
                            hasProperty('label', is(ann.snpName)),
                            hasProperty('snpName', is(ann.snpName)),
                    )
                }
        )

        // SnpDataByProbe properties
        that rows, contains(
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
                            hasProperty('a1a1Count', is(snpData.countA1A1)),
                            hasProperty('a1a2Count', is(snpData.countA1A2)),
                            hasProperty('a2a2Count', is(snpData.countA2A2)),
                            hasProperty('noCallCount', is(snpData.countNocall)),
                    )
                }
        )
    }

    void testFetchAllDataTestCells() {
        setupData()
        dataQueryResult = snpLzResource.retrieveData(
                [trialConstraint, conceptConstraint0], [], allDataProjection)

        List rows = Lists.newArrayList(dataQueryResult.rows)



        expect:
        /* Test correspondence between testData.assays and dataQueryResult.indicesList:
        * - test if the collection of assay ids returning from the query is
        *   the same as in the test data and that the assays are ordered by
        *   assay id.
        * - test if the subject ids (sampleCode) are also available in the
        *   retrieved assay data.
        */
        assert dataQueryResult.indicesList*.id == testData.orderedAssaysByPlatform[SnpLzTestData.PLATFORM]*.id
        assert dataQueryResult.indicesList*.sampleCode == testData.orderedAssaysByPlatform[SnpLzTestData.PLATFORM]*.sampleCode

        // test correspondence between testData and dataQueryResult rows
        rows.collect { row ->
            dataMatcherFor(row, testData.annotations, testData.orderedAssays)
        }
    }

    void testIndexingByNumber() {
        setupData()
        dataQueryResult = snpLzResource.retrieveData(
                [trialConstraint, conceptConstraint0], [], allDataProjection)

        List rows = Lists.newArrayList(dataQueryResult.rows)

        expect:
        // test correspondence between testData and dataQueryResult rows
        rows.each { row ->
            dataMatcherFor(row, testData.annotations, testData.orderedAssaysByPlatform[SnpLzTestData.PLATFORM])
        }
        SnpLzRow firstLzRow = rows.first()

        assert firstLzRow[1] == Lists.newArrayList(firstLzRow.iterator())[1]
    }

    void testIndexingByAssay() {
        setupData()
        dataQueryResult = snpLzResource.retrieveData(
                [trialConstraint, conceptConstraint0], [], allDataProjection)

        List rows = Lists.newArrayList(dataQueryResult.rows)

        expect:
        // test correspondence between testData and dataQueryResult rows
        rows.each { row ->
            dataMatcherFor(row, testData.annotations, testData.orderedAssaysByPlatform[SnpLzTestData.PLATFORM])
        }
        SnpLzRow firstLzRow = rows.first()

        assert firstLzRow[dataQueryResult.indicesList[1]] ==
                Lists.newArrayList(firstLzRow.iterator())[1]
    }

    void testFetchFilterBySample() {
        setupData()
        def selectedAssay = testData.assays[1]
        def assayConstraint = snpLzResource.createAssayConstraint(
                AssayConstraint.ASSAY_ID_LIST_CONSTRAINT,
                ids: [selectedAssay.id])
        dataQueryResult = snpLzResource.retrieveData(
                [trialConstraint, conceptConstraint0, assayConstraint], [], allDataProjection)

        expect:
        that dataQueryResult.indicesList,
                contains(hasSameInterfaceProperties(Assay, selectedAssay))

        List rows = Lists.newArrayList(dataQueryResult.rows)

        rows.collect { row ->
            dataMatcherFor(row, testData.annotations, [selectedAssay])
        }
    }

    void testFetchFilterDataBySnpName() {
        setupData()
        def selectedAnnotation = testData.annotations[2]
        def selectedSnpName = selectedAnnotation.snpName

        def dataConstraint = snpLzResource.createDataConstraint(
                'snps', names: [selectedSnpName])
        dataQueryResult = snpLzResource.retrieveData(
                [trialConstraint, conceptConstraint0], [dataConstraint], allDataProjection)

        List rows = Lists.newArrayList(dataQueryResult.rows)

        expect:
        // test correspondence between testData and dataQueryResult rows
        rows.each { row ->
            dataMatcherFor(row, [selectedAnnotation], testData.orderedAssaysByPlatform[SnpLzTestData.PLATFORM])
        }
    }

    void testFetchFilterDataByGene() {
        setupData()
        def gene = 'ND2'
        def selectedAnnotation =
                testData.annotations.find { it.geneInfo.contains(gene) }

        def dataConstraint = snpLzResource.createDataConstraint(
                DataConstraint.GENES_CONSTRAINT, names: [gene])
        dataQueryResult = snpLzResource.retrieveData(
                [trialConstraint, conceptConstraint0], [dataConstraint], allDataProjection)

        List rows = Lists.newArrayList(dataQueryResult.rows)

        expect:
        that rows, hasSize(1)
        // test correspondence between testData and dataQueryResult rows
        rows.each { row ->
            dataMatcherFor(row, [selectedAnnotation], testData.orderedAssaysByPlatform[SnpLzTestData.PLATFORM])
        }
    }

    void testFetchFilterByChromosomeLocation() {
        setupData()
        def locationConstraint = snpLzResource.createDataConstraint(
                DataConstraint.CHROMOSOME_SEGMENT_CONSTRAINT,
                chromosome: '1', start: 4100, end: 4250)
        dataQueryResult = snpLzResource.retrieveData(
                [trialConstraint, conceptConstraint0], [locationConstraint], allDataProjection)

        List rows = Lists.newArrayList(dataQueryResult.rows)

        expect:
        that rows, hasSize(2)
        // test correspondence between testData and dataQueryResult rows
        rows.each { row ->
            dataMatcherFor(row, testData.annotations[0..1], testData.orderedAssaysByPlatform[SnpLzTestData.PLATFORM])
        }
    }

    void testFindsAssays() {
        setupData()
        def assayConstraint = highDimensionResourceService.
                createAssayConstraint(AssayConstraint.TRIAL_NAME_CONSTRAINT,
                        name: SnpLzTestData.TRIAL_NAME)

        def res = highDimensionResourceService.
                getSubResourcesAssayMultiMap([assayConstraint])

        expect:
        that res, hasEntry(
                hasProperty('dataTypeName', is('snp_lz')),
                containsInAnyOrder(
                        testData.assays.collect {
                            hasSameInterfaceProperties(Assay, it)
                        }
                )
        )
    }

    void testAllelesProjection() {
        setupData()
        def (relevantRow, assaySampleCode, rsId) =
        otherProjectionsCommon('alleles')

        def realRsId = relevantRow.probeData['snpName']
        def assayRealCode = relevantRow.orderedAssayPatientIndex.keySet()[0].label

        expect:
        assert assaySampleCode == assayRealCode
        assert realRsId == rsId
    }

    void testProbabilitiesProjection() {
        setupData()
        String projection = 'probabilities'
        def (relevantRow, assaySampleCode, rsId) =
        otherProjectionsCommon(projection)

        expect:
        that relevantRow, contains(
                is(testData.sampleGps.get(assaySampleCode, rsId).split(' ').collect { it as double }
                        as SnpLzProbabilitiesCell)
        )
    }

    void testDoseProjection() {
        setupData()
        def (relevantRow, assaySampleCode, rsId) =
        otherProjectionsCommon('dose')

        expect:
        that relevantRow, contains(
                is(testData.sampleDoses.get(assaySampleCode, rsId) as double)
        )
    }

    def otherProjectionsCommon(String projectionName) {
        def projection = snpLzResource.createProjection(projectionName)
        def assay = testData.assays[0]
        def assaySampleCode = assay.sampleCode
        def rsId = testData.annotations[0].snpName

        def assayConstraint = snpLzResource.createAssayConstraint(
                AssayConstraint.ASSAY_ID_LIST_CONSTRAINT,
                ids: [assay.id])

        dataQueryResult = snpLzResource.retrieveData(
                [assayConstraint], [], projection)

        List rows = Lists.newArrayList(dataQueryResult.rows)

        def data = rows.getAt("probeData")
        def snpNames = data.collect { it ->
            it.get('snpName')
        }
        assert snpNames.contains(rsId)

        def relevantRow = rows.find { it.snpName == rsId }

        assert relevantRow != null


        [relevantRow, assaySampleCode, rsId]

    }

    // for all data projection only
    // Old way of testing the structure used groovy inside Hamcrest, this did not work anymore.
    // Therefore it now tests if the retrieved data is inside the specific testdata.
    def dataMatcherFor(row, annotations, assays) {
        def orderedSampleCodes = assays.findAll { it.platform.id == SnpLzTestData.PLATFORM }*.sampleCode

        def orderedSnpData = testData.data
                .findAll { it.bioAssayGenoPlatform.bioAssayPlatform.accession == SnpLzTestData.PLATFORM }
                .grep { it.genotypeProbeAnnotation.id in annotations*.id }
                .sort { it.genotypeProbeAnnotation.id }

        def genotypeProbeData = orderedSnpData.collect { it.genotypeProbeAnnotation }
        assert row.probeData['snpName'] in genotypeProbeData.collect { it['snpName'] }
        assert row.probeData['chromosome'] in genotypeProbeData.collect { it['chromosome'] }
        assert row.probeData['position'] in genotypeProbeData.collect { it['pos'] }

        def dataRow = row.collect()
        def gps = []
        def gts = []
        def doses = []
        orderedSnpData.collect { SnpDataByProbeCoreDb snpData ->
            orderedSampleCodes.collect { sampleCode ->
                gps.add(testData.sampleGps
                        .get(sampleCode, snpData.genotypeProbeAnnotation.snpName)
                        .split(' ')
                        .collect { it as Double })
                gts.add(testData.sampleGts
                        .get(sampleCode, snpData.genotypeProbeAnnotation.snpName)
                        .split(' '))
                doses.add(testData.sampleDoses
                        .get(sampleCode, snpData.genotypeProbeAnnotation.snpName) as double)
            }
        }

        if (dataRow.size() > 1) {
            dataRow.each { it ->
                checkBlob(it, gps, gts, doses)
            }
        } else {
            checkBlob(dataRow, gps, gts, doses)
        }
    }

    def checkBlob(dataRow, gps, gts, doses) {
        //Work around because when a putAt() is called on dataRow for testFetchFilterBySample, it is returning a list
        // with one element, while the others return it not within a list. Could be caused by some backend problem.
        List dataRowCheck = []
        boolean check = false
        dataRowCheck += dataRow['probabilityA1A1']
        dataRowCheck += dataRow['probabilityA1A2']
        dataRowCheck += dataRow['probabilityA2A2']
        assert dataRowCheck in gps
        gts.each { it ->
            dataRowCheck = []
            dataRowCheck += dataRow['likelyGenotype']
            if (dataRowCheck[0].split("_") == it) {
                check = true
            }
        }
        assert check
        dataRowCheck = []
        dataRowCheck += dataRow['minorAlleleDose']
        assert dataRowCheck[0] in doses
    }

}

