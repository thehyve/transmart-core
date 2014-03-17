package org.transmartproject.db.dataquery.highdim.vcf

import org.transmartproject.db.biomarker.BioMarkerCoreDb
import org.transmartproject.db.dataquery.highdim.DeGplInfo
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.dataquery.highdim.HighDimTestData
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.search.SearchKeywordCoreDb

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.notNullValue
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

/**
 * Created by j.hudecek on 13-3-14.
 */
class VcfTestData  {

    public static final String TRIAL_NAME = 'MRNA_SAMP_TRIAL'

    DeGplInfo platform = {
        def res = new DeGplInfo(
                title: 'Test VCF',
                organism: 'Homo Sapiens',
                markerTypeId: 'Variant data')
        res.id = 'BOGUSGPLVCF'
        res
    }()


    List<SearchKeywordCoreDb> searchKeywords = {
        bioMarkerTestData.geneSearchKeywords +
                bioMarkerTestData.proteinSearchKeywords +
                bioMarkerTestData.geneSignatureSearchKeywords
    }()

    List<DeMrnaAnnotationCoreDb> annotations = {
        def createAnnotation = { probesetId, probeId, BioMarkerCoreDb bioMarker ->
            def res = new DeMrnaAnnotationCoreDb(
                    gplId: platform.id,
                    probeId: probeId,
                    geneSymbol: bioMarker.name,
                    geneId: bioMarker.primaryExternalId,
                    organism: 'Homo sapiens',
            )
            res.id = probesetId
            res
        }
        [
                createAnnotation(-201, '1553506_at', bioMarkers[0]),
                createAnnotation(-202, '1553510_s_at', bioMarkers[1]),
                createAnnotation(-203, '1553513_at', bioMarkers[2]),
        ]
    }()

    List<PatientDimension> patients =
            HighDimTestData.createTestPatients(3, -300, TRIAL_NAME)

    List<DeSubjectSampleMapping> assays =
            HighDimTestData.createTestAssays(patients, -400, platform, TRIAL_NAME)

    List<DeVariantSubjectDetailCoreDb> detailsData = {
        def createDetail = {
                       int position,
                       char reference,
                       char alternative,
                       String info
                        ->
            new DeVariantSubjectDetailCoreDb(
                    chr: 1,
                    pos: position,
                    rsId: '.',
                    ref: reference,
                    alt: alternative,
                    quality: position/2, //nonsensical value
                    filter: '.',
                    info:  info,
                    format: ''
            )
        }
        [
                createDetail(1, 'C', 'A', 'DP=88;AF1=1;QD=2;DP4=0,0,80,0;MQ=60;FQ=-268'),
                createDetail(2, 'GCCCCC', 'GCCCC', 'DP=88;AF1=1;QD=2;DP4=0,0,80,0;MQ=60;FQ=-268'),
                createDetail(3, 'A', 'C', 'DP=88;AF1=1;QD=2;DP4=0,0,80,0;MQ=60;FQ=-268')
        ]
    }

    List<DeVariantSubjectSummaryCoreDb> summariesData = {
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
                        allele2: allele2
                )
        }

        def res = []
        detailsData.each { detail ->
            int mut = 0
            assays.each { assay ->
                mut++
                res += createSummary detail, mut&1, mut&2,  assay
            }
        }

        res
    }()


    void saveAll() {
        bioMarkerTestData.saveGeneData()

        assertThat platform.save(), is(notNullValue(DeGplInfo))
        save patients
        save assays
        save detailsData
        save summariesData
    }
}
