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

import groovy.util.logging.Slf4j
import spock.lang.Specification

import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.projections.AllDataProjection
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.dataquery.highdim.acgh.AcghTestData
import org.transmartproject.db.dataquery.highdim.metabolite.MetaboliteTestData
import org.transmartproject.db.dataquery.highdim.mirna.MirnaQpcrTestData
import org.transmartproject.db.dataquery.highdim.mirna.MirnaSeqTestData
import org.transmartproject.db.dataquery.highdim.mrna.MrnaTestData
import org.transmartproject.db.dataquery.highdim.protein.ProteinTestData
import org.transmartproject.db.dataquery.highdim.rbm.RbmTestData
import org.transmartproject.db.dataquery.highdim.rnaseq.RnaSeqTestData
import org.transmartproject.db.dataquery.highdim.rnaseqcog.RnaSeqCogTestData
import org.transmartproject.db.dataquery.highdim.tworegion.TwoRegionTestData
import org.transmartproject.db.dataquery.highdim.vcf.VcfTestData

import static org.hamcrest.Matchers.*

@RunWith(Parameterized)
@Slf4j
class HighDimensionAllDataSpec extends Specification {

    HighDimensionResource highDimensionResourceService

    String typename
    HighDimensionDataTypeResource type
    Map<String, Class> dataProperties
    Map<String, Class> rowProperties
    def testData

    @Parameters
    static Collection<Object[]> getParameters() { return [
        [
            'metabolite',
            [rawIntensity: double, logIntensity: double, zscore: double],
            [hmdbId:String, biochemicalName:String],
            MetaboliteTestData
        ], [
            'mirnaqpcr',
            [rawIntensity:BigDecimal, logIntensity:BigDecimal, zscore:BigDecimal],
            [probeId:String, mirnaId:String],
            MirnaQpcrTestData
        ], [
            'mirnaseq',
            [rawIntensity:BigDecimal, logIntensity:BigDecimal, zscore:BigDecimal],
            [probeId:String, mirnaId:String],
            MirnaSeqTestData
        ], [
            'mrna',
            [trialName:String, rawIntensity:BigDecimal, logIntensity: BigDecimal, zscore:BigDecimal],
            [probe:String, geneId:String, geneSymbol:String],
            MrnaTestData
        ], [
            'protein',
            [intensity:BigDecimal, logIntensity:BigDecimal, zscore:BigDecimal],
            [uniprotName:String, peptide:String],
            ProteinTestData
        ], [
            'rbm',
            [value:BigDecimal, logIntensity:BigDecimal, zscore:BigDecimal],
            [antigenName:String, unit:String, uniprotName:String],
            RbmTestData
        ], [
            'rnaseq_cog',
            [rawIntensity:BigDecimal, logIntensity:BigDecimal, zscore:BigDecimal],
            [annotationId:String, geneSymbol:String, geneId:String],
            RnaSeqCogTestData
        ], [
            'two_region',
            [downChromosome:String, upChromosome: String, id:Long, upEnd:Long, upPos:Long, upStrand:Character, downEnd:Long, downPos:Long, downStrand:Character, isInFrame: Boolean],
            [:],
            TwoRegionTestData
        ], [
            'vcf',
            [reference:Boolean, variant:String, variantType:String],
            [chromosome:String, position:Long, rsId: String, referenceAllele:String],
            VcfTestData
        ], [
            'acgh',
            [chipCopyNumberValue:Double, segmentCopyNumberValue:Double, flag:Short,
             probabilityOfLoss:Double, probabilityOfNormal:Double, probabilityOfGain:Double,
             probabilityOfAmplification:Double ],
            [id: Long, name:String, cytoband:String, chromosome:String, start:Long, end:Long, numberOfProbes:Integer,
             bioMarker: String],
            AcghTestData
        ], [
            'rnaseq',
            [readcount:Integer, normalizedReadcount:Double, logNormalizedReadcount:Double, zscore:Double],
            [id: Long, name:String, cytoband:String, chromosome:String, start:Long, end:Long, numberOfProbes:Integer, bioMarker: String],
            RnaSeqTestData
        ]
    ].collect {it.toArray()}}

    void setupData() {
        type = highDimensionResourceService.getSubResourceForType(typename)
        assert type != null
        assert testData != null
        testData.saveAll()
    }

    void testDescription() {
        setupData()
        expect: type.dataTypeDescription instanceOf(String)
    }

    void testAllDataProjection() {
        setupData()
        AllDataProjection genericProjection = type.createProjection(Projection.ALL_DATA_PROJECTION)

        def result = type.retrieveData([], [], genericProjection)
        try {
            when:
                def firstrow = result.iterator().next()
            then:
                firstrow is(notNullValue())

            rowProperties.each { prop, type ->
                when:
                    true
                then:
                    genericProjection.rowProperties hasKey(prop)
                    genericProjection.rowProperties[prop] is((Object) type)
            }
            genericProjection.rowProperties.each { prop, type ->
                when:
                    true
                then:
                    firstrow hasProperty(prop)
                when:
                    true
                then: "${owner.type.dataTypeName}: $prop is not of expected type."
                    firstrow."$prop".getClass() typeCompatibleWith(type)
            }

            when:
                def data = firstrow.find()
            then:
                data is(notNullValue())

            dataProperties.each { col, type ->
                when:
                    true
                then:
                    genericProjection.dataProperties hasKey(col)
                    genericProjection.dataProperties[col] is((Object) type)
            }

            genericProjection.dataProperties.each { String col, Class type ->
                when:
                    true
                then: data anyOf(
                        hasKey(col),
                        hasProperty(col))

                when:
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
                then: "${owner.type.dataTypeName}: $col is not of expected type."
                    data."$col".getClass() typeCompatibleWith(targetType)
            }
        } finally {
            result?.close()
        }
    }

}
