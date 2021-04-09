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

package org.transmartproject.db.dataquery.highdim

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.projections.AllDataProjection
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.TestData
import org.transmartproject.db.dataquery.highdim.acgh.AcghTestData
import org.transmartproject.db.dataquery.highdim.metabolite.MetaboliteTestData
import org.transmartproject.db.dataquery.highdim.mirna.MirnaQpcrTestData
import org.transmartproject.db.dataquery.highdim.mirna.MirnaSeqTestData
import org.transmartproject.db.dataquery.highdim.mrna.MrnaTestData
import org.transmartproject.db.dataquery.highdim.protein.ProteinTestData
import org.transmartproject.db.dataquery.highdim.rbm.RbmTestData
import org.transmartproject.db.dataquery.highdim.rnaseq.RnaSeqTestData
import org.transmartproject.db.dataquery.highdim.rnaseq.transcript.RnaSeqTranscriptTestData
import org.transmartproject.db.dataquery.highdim.rnaseqcog.RnaSeqCogTestData
import org.transmartproject.db.dataquery.highdim.tworegion.TwoRegionTestData
import org.transmartproject.db.dataquery.highdim.vcf.VcfTestData
import spock.lang.Specification

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@Integration
@Rollback
class HighDimensionAllDataTests extends Specification {

    @Autowired
    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource type
    Map<String, Class> dataProperties
    Map<String, Class> rowProperties

    void setupData() {
        TestData.prepareCleanDatabase()
    }

    void testMetaboliteRetrieval() {
        setupData()
        type = highDimensionResourceService.getSubResourceForType('metabolite')
        dataProperties = [rawIntensity: double, logIntensity: double, zscore: double]
        rowProperties = [hmdbId: String, biochemicalName: String]
        new MetaboliteTestData().saveAll()

        testRetrieval()
    }

    void testMirnaQpcrRetrieval() {
        setupData()
        type = highDimensionResourceService.getSubResourceForType('mirnaqpcr')
        dataProperties = [rawIntensity: BigDecimal, logIntensity: BigDecimal, zscore: BigDecimal]
        rowProperties = [probeId: String, mirnaId: String]
        new MirnaQpcrTestData().saveAll()

        testRetrieval()
    }

    void testMirnaSeqRetrieval() {
        setupData()
        type = highDimensionResourceService.getSubResourceForType('mirnaseq')
        dataProperties = [rawIntensity: BigDecimal, logIntensity: BigDecimal, zscore: BigDecimal]
        rowProperties = [probeId: String, mirnaId: String]
        new MirnaSeqTestData().saveAll()

        testRetrieval()
    }

    void testMrnaRetrieval() {
        setupData()
        type = highDimensionResourceService.getSubResourceForType('mrna')
        dataProperties = [trialName: String, rawIntensity: BigDecimal, logIntensity: BigDecimal, zscore: BigDecimal]
        rowProperties = [probe: String, geneId: String, geneSymbol: String]
        new MrnaTestData().saveAll()

        testRetrieval()
    }

    void testProteinRetrieval() {
        setupData()
        type = highDimensionResourceService.getSubResourceForType('protein')
        dataProperties = [intensity: BigDecimal, logIntensity: BigDecimal, zscore: BigDecimal]
        rowProperties = [uniprotName: String, peptide: String]
        new ProteinTestData().saveAll()

        testRetrieval()
    }

    void testRbmRetrieval() {
        setupData()
        type = highDimensionResourceService.getSubResourceForType('rbm')
        dataProperties = [value: BigDecimal, logIntensity: BigDecimal, zscore: BigDecimal]
        rowProperties = [antigenName: String, unit: String, uniprotName: String]
        new RbmTestData().saveAll()

        testRetrieval()
    }

    void testRnaSeqCogRetrieval() {
        setupData()
        type = highDimensionResourceService.getSubResourceForType('rnaseq_cog')
        dataProperties = [rawIntensity: BigDecimal, logIntensity: BigDecimal, zscore: BigDecimal]
        rowProperties = [annotationId: String, geneSymbol: String, geneId: String]
        new RnaSeqCogTestData().saveAll()

        testRetrieval()
    }

    void testTwoRegionRetrieval() {
        setupData()
        type = highDimensionResourceService.getSubResourceForType('two_region')
        dataProperties = [downChromosome: String, upChromosome: String, id: Long, upEnd: Long, upPos: Long,
                          upStrand      : Character, downEnd: Long, downPos: Long, downStrand: Character, isInFrame: Boolean]
        rowProperties = [:]
        new TwoRegionTestData().saveAll()

        testRetrieval()
    }

    void testVcfRetrieval() {
        setupData()
        type = highDimensionResourceService.getSubResourceForType('vcf')
        dataProperties = [reference: Boolean, variant: String, variantType: String]
        rowProperties = [chromosome: String, position: Long, rsId: String, referenceAllele: String]
        new VcfTestData().saveAll()

        testRetrieval()
    }

    void testAcghRetrieval() {
        setupData()
        type = highDimensionResourceService.getSubResourceForType('acgh')
        dataProperties = [chipCopyNumberValue       : Double, segmentCopyNumberValue: Double, flag: Short,
                          probabilityOfLoss         : Double, probabilityOfNormal: Double, probabilityOfGain: Double,
                          probabilityOfAmplification: Double]
        rowProperties = [id       : Long, name: String, cytoband: String, chromosome: String, start: Long, end: Long, numberOfProbes: Integer,
                         bioMarker: String]
        new AcghTestData().saveAll()

        testRetrieval()
    }

    void testRnaSeqRetrieval() {
        setupData()
        type = highDimensionResourceService.getSubResourceForType('rnaseq')
        dataProperties = [readcount: Integer, normalizedReadcount: Double, logNormalizedReadcount: Double, zscore: Double]
        rowProperties = [id            : Long, name: String, cytoband: String, chromosome: String, start: Long, end: Long,
                         numberOfProbes: Integer, bioMarker: String]
        new RnaSeqTestData().saveAll()

        testRetrieval()
    }

    void testRnaSeqTranscriptRetrieval() {
        setupData()
        type = highDimensionResourceService.getSubResourceForType('rnaseq_transcript')
        dataProperties = [readcount: Integer, normalizedReadcount: Double, logNormalizedReadcount: Double, zscore: Double]
        rowProperties = [id: Long, chromosome: String, start: Long, end: Long, bioMarker: String]
        new RnaSeqTranscriptTestData().saveAll()

        testRetrieval()
    }

    void testRetrieval() {
        assertThat type.dataTypeDescription, instanceOf(String)

        AllDataProjection genericProjection = type.createProjection(Projection.ALL_DATA_PROJECTION)

        def result = type.retrieveData([], [], genericProjection)
        try {
            def firstrow = result.iterator().next()

            assertThat firstrow, is(notNullValue())
            rowProperties.each { prop, type ->
                assertThat genericProjection.rowProperties, hasKey(prop)
                assertThat genericProjection.rowProperties[prop], is((Object) type)
            }
            genericProjection.rowProperties.each { prop, type ->
                assertThat firstrow, hasProperty(prop)
                assertThat "${owner.type.dataTypeName}: $prop is not of expected type.",
                        firstrow."$prop".getClass(), typeCompatibleWith(type)
            }

            def data = firstrow.find()

            assertThat data, is(notNullValue())
            dataProperties.each { col, type ->
                assertThat genericProjection.dataProperties, hasKey(col)
                assertThat genericProjection.dataProperties[col], is((Object) type)
            }
            genericProjection.dataProperties.each { String col, Class type ->
                assertThat data, anyOf(
                        hasKey(col),
                        hasProperty(col))

                def targetType = type
                if (type.isPrimitive()) {
                    // groovy will box the primitive next, so convert the type
                    // to the boxed one
                    switch (type) {
                        case Double.TYPE:
                            targetType = Double
                            break
                        default:
                            throw new UnsupportedOperationException()
                    }
                }

                assertThat "${owner.type.dataTypeName}: $col is not of expected type.",
                        data."$col".getClass(), typeCompatibleWith(targetType)
            }
        } finally {
            result?.close()
        }
    }

}
