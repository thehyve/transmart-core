package org.transmartproject.db.dataquery.highdim

import grails.test.mixin.TestMixin
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.projections.AllDataProjection
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.dataquery.highdim.metabolite.MetaboliteTestData
import org.transmartproject.db.dataquery.highdim.mirna.MirnaQpcrTestData
import org.transmartproject.db.dataquery.highdim.mirna.MirnaSeqTestData
import org.transmartproject.db.dataquery.highdim.mrna.MrnaTestData
import org.transmartproject.db.dataquery.highdim.protein.ProteinTestData
import org.transmartproject.db.dataquery.highdim.rbm.RbmTestData
import org.transmartproject.db.dataquery.highdim.rnaseqcog.RnaSeqCogTestData
import org.transmartproject.db.dataquery.highdim.vcf.VcfTestData
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@RunWith(Parameterized)
@TestMixin(RuleBasedIntegrationTestMixin)
class HighDimensionAllDataTests {

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
            [rawIntensity: BigDecimal, logIntensity: BigDecimal, zscore: BigDecimal],
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
            'vcf',
            [reference:Boolean, variant:String, variantType:String],
            [chromosome:String, position:Long, rsId: String, referenceAllele:String],
            VcfTestData
        ]
    ].collect {it.toArray()}}

    HighDimensionAllDataTests(String typename, Map<String, Class> dataProperties,
            Map<String, Class> rowProperties, Class testData) {
        this.typename = typename
        this.dataProperties = dataProperties
        this.rowProperties = rowProperties
        this.testData = testData.newInstance()
    }

    @Before
    void setUp() {
        type = highDimensionResourceService.getSubResourceForType(typename)
        assertThat type, is(notNullValue())
        assertThat testData, is(notNullValue())
        testData.saveAll()
    }

    @Test
    void testDescription() {
        assertThat type.dataTypeDescription, instanceOf(String)
    }

    @Test
    void testAllDataProjection() {
        AllDataProjection genericProjection = type.createProjection(Projection.ALL_DATA_PROJECTION)

        def result = type.retrieveData([], [], genericProjection)
        try {
            def indicesList = result.indicesList
            def firstrow = result.iterator().next()

            assertThat firstrow, is(notNullValue())
            rowProperties.each { prop, type ->
                assertThat genericProjection.rowProperties, hasKey(prop)
                assertThat genericProjection.rowProperties[prop], is((Object) type)
            }
            genericProjection.rowProperties.each { prop, type ->
                assertThat firstrow, hasProperty(prop)
                assertThat firstrow."$prop".getClass(), typeCompatibleWith(type)
            }

            def data = firstrow[indicesList[0]]

            assertThat data, is(notNullValue())
            assertThat data, is(instanceOf(Map))
            dataProperties.each { col, type ->
                assertThat genericProjection.dataProperties, hasKey(col)
                assertThat genericProjection.dataProperties[col], is((Object) type)
            }
            genericProjection.dataProperties.each { col, type ->
                assertThat data, hasKey(col)
                for(int i in 1..5) {
                    println('**************************************************************************************')
                }
                println("key: $col, value: ${data."$col"}, type: ${data."$col".getClass()}")
                assertThat data."$col".getClass(), typeCompatibleWith(type)
            }
        } finally {
            result?.close()
        }
    }

}
