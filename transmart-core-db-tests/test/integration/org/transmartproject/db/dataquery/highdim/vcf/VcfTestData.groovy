package org.transmartproject.db.dataquery.highdim.vcf

import org.transmartproject.db.dataquery.highdim.*
import org.transmartproject.db.i2b2data.PatientDimension

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.notNullValue
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

/**
 * Created by j.hudecek on 13-3-14.
 */
class VcfTestData  {

    public static final String TRIAL_NAME = 'VCF_SAMP_TRIAL'

    DeGplInfo platform
    DeVariantDatasetCoreDb dataset
    List<PatientDimension> patients
    List<DeSubjectSampleMapping> assays
    List<DeVariantSubjectSummaryCoreDb> summariesData
    List<DeVariantSubjectDetailCoreDb> detailsData

    public VcfTestData() {
        platform = new DeGplInfo(
                    title: 'Test VCF',
                    organism: 'Homo Sapiens',
                    markerTypeId: 'Variant data')
        platform.id = 'BOGUSGPLVCF'
        dataset = new DeVariantDatasetCoreDb(genome:'human')
        dataset.id = 'BOGUSDTST'
        patients = HighDimTestData.createTestPatients(3, -800, TRIAL_NAME)
        assays = HighDimTestData.createTestAssays(patients, -1400, platform, TRIAL_NAME)
        detailsData = []
        detailsData += createDetail(1, 'C', 'A', 'DP=88;AF1=1;QD=2;DP4=0,0,80,0;MQ=60;FQ=-268')
        detailsData += createDetail(2, 'GCCCCC', 'GCCCC', 'DP=88;AF1=1;QD=2;DP4=0,0,80,0;MQ=60;FQ=-268')
        detailsData += createDetail(3, 'A', 'C,T', 'DP=88;AF1=1;QD=2;DP4=0,0,80,0;MQ=60;FQ=-268')

        summariesData = []
        detailsData.each { detail ->
            int mut = 0
            assays.each { assay ->
                mut++
                summariesData += createSummary detail, mut&1, (mut&2)>>1,  assay
            }
            if (detail.alt.contains(','))
                summariesData.last().allele1=2
        }
    }

    def createDetail = {
        int position,
        String reference,
        String alternative,
        String info
            ->
            new DeVariantSubjectDetailCoreDb(
                    chr: 1,
                    pos: position,
                    rsId: '.',
                    ref: reference,
                    alt: alternative,
                    quality: position, //nonsensical value
                    filter: '.',
                    info:  info,
                    format: '',
                    dataset: dataset
            )
    }

    def createSummary = {
        DeVariantSubjectDetailCoreDb detail,
        int allele1,
        int allele2,
        DeSubjectSampleMapping assay
            ->
            new DeVariantSubjectSummaryCoreDb(
                    chr: 1,
                    pos: detail.pos,
                    rsId: '.',
                    variant: (allele1 ? detail.ref : detail.alt) + '/' + (allele2 ? detail.ref : detail.alt),
                    variantFormat: (allele1 ? 'R':'V') + '/' + (allele2 ? 'R':'V'),
                    variantType: detail.ref.length()>1?'DIV':'SNV',
                    reference: true,
                    allele1: allele1,
                    allele2: allele2,
                    subjectId: assay.sampleCode,
                    dataset: dataset,
                    assay: assay,
                    jDetail: detail
            )
    }




    void saveAll() {
        assertThat platform.save(), is(notNullValue(DeGplInfo))
        save([dataset])
        save patients
        save assays
        save detailsData
        save summariesData
    }
}
