package org.transmartproject.db.dataquery.highdim.vcf

import grails.test.mixin.TestMixin
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.chromoregion.RegionRow
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.db.test.Matchers
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Created by j.hudecek on 17-3-14.
 */
@TestMixin(RuleBasedIntegrationTestMixin)
class VcfEndToEndRetrievalTests {


    private static final double DELTA = 0.0001
    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource vcfResource

    Closeable dataQueryResult

    VcfTestData testData = new VcfTestData()

    AssayConstraint trialNameConstraint

    @Before
    void setUp() {
        testData.saveAll()

        vcfResource = highDimensionResourceService.getSubResourceForType 'cohortMAF'
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
    void basicTest() {
        List dataConstraints = []
        def projection = vcfResource.createProjection [:], 'cohortMAF_values'

        dataQueryResult = vcfResource.retrieveData(
                [], dataConstraints, projection)

        def resultList = []
        for (def region : dataQueryResult.rows) {
            resultList += region[0][0]
        }

        assertThat resultList, hasSize(3)

        assertThat resultList, everyItem(allOf(
                hasEntry(equalTo('chr'), equalTo("1")),
                hasEntry(equalTo('rsId'), equalTo(".")),
                hasEntry(equalTo('filter'), equalTo(".")),
                hasEntry(equalTo('format'), equalTo("")),
                hasKey('quality'),
                hasKey('pos')
        )

        )
        assertThat resultList, everyItem(
                Matchers.hasEqualValueProperties('pos', 'quality')
        )
        assertThat resultList, hasItem(
                allOf(
                        hasEntry(equalTo('pos'), equalTo(1L)),
                        hasEntry(equalTo('ref'), equalTo('C')),
                        hasEntry(equalTo('alt'), equalTo('A'))
                )
        )
        assertThat resultList, hasItem(
                allOf(
                        hasEntry(equalTo('pos'), equalTo(2L)),
                        hasEntry(equalTo('ref'), equalTo('GCCCCC')),
                        hasEntry(equalTo('alt'), equalTo('GCCCC'))
                )
        )
        assertThat resultList, hasItem(
                allOf(
                        hasEntry(equalTo('pos'), equalTo(3L)),
                        hasEntry(equalTo('ref'), equalTo('A')),
                        hasEntry(equalTo('alt'), equalTo('C,T'))
                )
        )
    }

    @Test
    void basicTestWithConstraints() {
        List dataConstraints = [vcfResource.createDataConstraint(
                DataConstraint.DISJUNCTION_CONSTRAINT,
                subconstraints: [
                        (DataConstraint.CHROMOSOME_SEGMENT_CONSTRAINT): [chromosome: "1", start: 1, end: 2]
                ]
        )]
        def projection = vcfResource.createProjection [:], 'cohortMAF_values'

        dataQueryResult = vcfResource.retrieveData(
                [], dataConstraints, projection)

        def resultList = []
        for (def region : dataQueryResult.rows) {
            resultList += region[0][0]
        }

        assertThat resultList, hasSize(2)

        assertThat resultList, everyItem(allOf(
                hasEntry(equalTo('chr'), equalTo("1")),
                hasEntry(equalTo('rsId'), equalTo(".")),
                hasEntry(equalTo('filter'), equalTo(".")),
                hasEntry(equalTo('format'), equalTo("")),
                hasKey('quality'),
                hasKey('pos')
        )

        )
        assertThat resultList, everyItem(
                Matchers.hasEqualValueProperties('pos', 'quality')
        )
        assertThat resultList, hasItem(
                allOf(
                        hasEntry(equalTo('pos'), equalTo(1L)),
                        hasEntry(equalTo('ref'), equalTo('C')),
                        hasEntry(equalTo('alt'), equalTo('A'))
                )
        )
        assertThat resultList, hasItem(
                allOf(
                        hasEntry(equalTo('pos'), equalTo(2L)),
                        hasEntry(equalTo('ref'), equalTo('GCCCCC')),
                        hasEntry(equalTo('alt'), equalTo('GCCCC'))
                )
        )
        assertThat resultList, not(hasItem(
                allOf(
                        hasEntry(equalTo('pos'), equalTo(3L)),
                        hasEntry(equalTo('ref'), equalTo('A')),
                        hasEntry(equalTo('alt'), equalTo('C,T'))
                )
        )
        )
    }

    @Test
    void basicTestWithCalculation() {
        List dataConstraints = []
        def projection = vcfResource.createProjection [:], 'cohortMAF_values'

        dataQueryResult = vcfResource.retrieveData(
                [], dataConstraints, projection)

        def resultList = []
        for (RegionRow region : dataQueryResult.rows) {
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
            createDetail(3, 'A', 'C', 'DP=88;AF1=1;QD=2;DP4=0,0,80,0;MQ=60;FQ=-268')
        */
        //{assayId=-1403, allele2=2, allele1=1, format=, variant=null, reference=true, variantType=SNV,
        // info=DP=88;AF1=1;QD=2;DP4=0,0,80,0;MQ=60;FQ=-268, pos=1, ref=C, rsId=., alt=A, subjectId=SAMPLE_FOR_-803,
        // quality=1, variantFormat=R/R, filter=., chr=1}
        assertThat resultList, hasItem(
                allOf(
                        hasProperty('position', equalTo(1L)),
                        hasProperty('mafAllele', equalTo('A')),
                        hasProperty('maf', closeTo(0.666 as Double, 0.1 as Double)),
                        hasProperty('genomicVariantTypes', contains(org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType.SNP)),
                        hasProperty('alternativeAlleles', contains('A')),
                        hasProperty('additionalInfo', allOf(
                                hasEntry(equalTo('DP'),equalTo('88')),
                                hasEntry(equalTo('AF1'),equalTo('1')),
                                hasEntry(equalTo('QD'),equalTo('2')),
                                hasEntry(equalTo('DP4'),equalTo('0,0,80,0')),
                                hasEntry(equalTo('MQ'),equalTo('60')),
                                hasEntry(equalTo('FQ'),equalTo('-268')),
                                //computed values
                                hasEntry(equalTo('AC'),equalTo('4')),
                                hasEntry(equalTo('AN'),equalTo('6')),

                        ))
                )
        )
        assertThat resultList, hasItem(
                allOf(
                        hasProperty('position', equalTo(2L)),
                        hasProperty('mafAllele', equalTo('GCCCC')),
                        hasProperty('maf', closeTo(0.666 as Double, 0.1 as Double)),
                        hasProperty('genomicVariantTypes', contains(org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType.DEL)),
                        hasProperty('alternativeAlleles', contains('GCCCC'))

                )
        )
        assertThat resultList, hasItem(
                allOf(
                        hasProperty('position', equalTo(3L)),
                        hasProperty('mafAllele', equalTo('C')),
                        hasProperty('maf', closeTo(0.5 as Double, 0.1 as Double)),
                        hasProperty('genomicVariantTypes', contains(org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType.SNP,
                                org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType.SNP)),
                        hasProperty('alternativeAlleles', containsInAnyOrder('C', 'T')),
                        hasProperty('additionalInfo', allOf(
                                hasEntry(equalTo('DP'),equalTo('88')),
                                hasEntry(equalTo('AF1'),equalTo('1')),
                                hasEntry(equalTo('QD'),equalTo('2')),
                                hasEntry(equalTo('DP4'),equalTo('0,0,80,0')),
                                hasEntry(equalTo('MQ'),equalTo('60')),
                                hasEntry(equalTo('FQ'),equalTo('-268')),
                                //computed values
                                hasEntry(equalTo('AC'),equalTo('1,3')),
                                hasEntry(equalTo('AN'),equalTo('6')),

                        ))
                )
        )





    }
}
