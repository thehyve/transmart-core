package org.transmartproject.db.dataquery.highdim.mrna

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.transmartproject.db.bioassay.BioAssayDataAnnotation
import org.transmartproject.db.bioassay.BioAssayFeatureGroup
import org.transmartproject.db.biomarker.BioMarker
import org.transmartproject.db.dataquery.highdim.DeGplInfo
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.dataquery.highdim.HighDimTestData
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.search.SearchGeneSignature
import org.transmartproject.db.search.SearchGeneSignatureItem
import org.transmartproject.db.search.SearchKeyword
import org.transmartproject.db.user.SearchAuthPrincipal
import org.transmartproject.db.user.SearchAuthUser

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

class MrnaTestData {

    public static final String TRIAL_NAME = 'MRNA_SAMP_TRIAL'

    static DeGplInfo platform = {
        def res = new DeGplInfo(
                title: 'Affymetrix Human Genome U133A 2.0 Array',
                organism: 'Homo Sapiens',
                markerTypeId: 'Gene Expression')
        res.id = 'BOGUSGPL570'
        res
    }()

    static List<BioMarker> bioMarkers = {
        def common = [
                organism: 'HOME SAPIENS',
                primarySourceCode: 'Entrez',
                bioMarkerType: 'GENE',
        ]
        def res = [
                new BioMarker(
                        bioMarkerName: 'BOGUSCPO',
                        bioMarkerDescription: 'carboxypeptidase O',
                        primaryExternalId: '-130749',
                        *: common,
                ),
                new BioMarker(
                        bioMarkerName: 'BOGUSRQCD1',
                        bioMarkerDescription: 'RCD1 required for cell differentiation1 homolog (S. pombe)',
                        primaryExternalId: '-9125',
                        *: common,
                ),
                new BioMarker(
                        bioMarkerName: 'BOGUSVNN3',
                        bioMarkerDescription: 'vanin 3',
                        primaryExternalId: '-55350',
                        *: common,
                ),
        ]
        res[0].id = -101
        res[1].id = -102
        res[2].id = -103

        res
    }()

//    static List<BioMarkerCorrelMv> bioMarkerCorrelations = {
//        def createCorrelation = { BioMarker b1, BioMarker b2 ->
//            new BioMarkerCorrelMv(
//                    bioMarker: b1,
//                    associatedBioMarker: b2,
//                    correlationType: 'GENE',
//                    mvId: 1
//            )
//        }
//
//        def res = []
//        /* do the self correlations... */
//        bioMarkers.each {
//            res += createCorrelation it, it
//        }
//        res += createCorrelation bioMarkers[0] /* CPO */, bioMarkers[1] /* RQCD1 */ /* no real-world relationship... */
//
//        res
//    }()

    static List<SearchAuthPrincipal> principals = {
        def res = [
               new SearchAuthPrincipal(enabled: true)
        ]
        res[0].id = -1001
        res
    }()

    static List<SearchAuthUser> users = {
        def res = [
                new SearchAuthUser(username: 'foobar')
        ]
        res[0].id = principals[0].id
        res
    }()

    static List<SearchGeneSignature> geneSignatures = {
        /* only id and deletedFlag are important.
         * we also have to fill some not-null fields */
        def createGeneSignature = { id ->
            def res = new SearchGeneSignature(
                    deletedFlag: false,
                    name: 'bogus_gene_sig_' + id,
                    uploadFile: 'bogus_upload_file',
                    speciesConceptId: '0',
                    creator: users[0],
                    createDate: new Date(),
                    bioAssayPlatformId: 0,
                    PValueCutoffConceptId: 0,
            )
            res.id = id
            res
        }

        (-602..-601).reverse().collect {
            createGeneSignature it
        }
    }()

    static List<BioAssayFeatureGroup> assayFeatureGroups = {
        (-702..-701).reverse().collect {
            def res = new BioAssayFeatureGroup(
                    name: 'probeSet' + it,
                    type: 'foobar'
            )
            res.id = it
            res
        }
    }()

    static List<BioAssayDataAnnotation> assayAnnotations = {
        [
                new BioAssayDataAnnotation(
                        probeSet: assayFeatureGroups[0],
                        bioMarker: bioMarkers[1],
                ),
                new BioAssayDataAnnotation(
                        probeSet: assayFeatureGroups[1],
                        bioMarker: bioMarkers[0],
                ),
        ]
    }()

    static List<SearchGeneSignatureItem> geneSignatureItems = {
        def createGeneSignatureItem = { BioMarker bioMarker,
                                        SearchGeneSignature geneSignature,
                                        Long foldChangeMetric,
                                        BioAssayFeatureGroup probeSet,
                                        id ->
            def res = new SearchGeneSignatureItem(
                bioMarker: bioMarker,
                geneSignature: geneSignature,
                foldChangeMetric: foldChangeMetric,
                probeSet: probeSet
            )
            res.id = id
            res
        }

        [
                createGeneSignatureItem(bioMarkers[0], geneSignatures[0], -1L, assayFeatureGroups[0], -901),
                createGeneSignatureItem(bioMarkers[1], geneSignatures[0], 0L,  assayFeatureGroups[1], -902),
                createGeneSignatureItem(bioMarkers[2], geneSignatures[1], 1L,  assayFeatureGroups[0], -903),
        ]
    }()

    /* The view SEARCH_BIO_MKR_CORREL_VIEW associates
     * gene signature ids with bio marker ids in two ways:
     *
     *   1. take the bio_marker_id from the gene signature's items
     *   2. take the bio assay feature groups associated with the gene
     *      signature's items and then take the bio marker ids of the
     *      annotations for those feature groups
     *
     * Therefore, the view should have the following associations after this
     * test data is inserted:
     *
     * Gene signature -601:
     *   item -901 -> bioMarker -101 (BOGUSCPO)
     *   item -902 -> bioMarker -102 (BOGUSRQCD1)
     *   item -901 -> probeSet -701 -> annotation #0 -> bioMarker -102 (BOGUSRQCD1)
     *   item -902 -> probeSet -702 -> annotation #1 -> bioMarker -101 (BOGUSCPO)
     *
     * Gene signature -602:
     *   item -903 -> bioMarker -103 (BOGUSVNN3)
     *   item -903 -> probeSet -701 -> annotation #0 -> bioMarker -102 (BOGUSRQCD1)
     */

    static List<SearchKeyword> searchKeywords = {
        def createGeneKeyword = { BioMarker gene, id ->
            def res = new SearchKeyword(
                    keyword: gene.bioMarkerName,
                    bioDataId: gene.id,
                    uniqueId: "GENE:$gene.primaryExternalId",
                    dataCategory: 'GENE',
            )
            res.id = id
            res
        }
        def createGeneSignatureKeyword = { SearchGeneSignature sig, id ->
            def res = new SearchKeyword(
                    keyword: "genesig_keyword_$sig.id",  /* what should this look like? */
                    bioDataId: sig.id,
                    uniqueId: "GENESIG:$sig.id", /* what should this look like? */
                    dataCategory: 'GENESIG',     /* what should this look like? */
            )
            res.id = id
            res
        }

        int i = -500;
        bioMarkers.collect {
            createGeneKeyword it, --i
        } +
        geneSignatures.collect {
            createGeneSignatureKeyword it, --i
        }
    }()

    static List<DeMrnaAnnotationCoreDb> annotations = {
        def createAnnotation = { probesetId, probeId, BioMarker bioMarker ->
            def res = new DeMrnaAnnotationCoreDb(
                    gplId: platform.id,
                    probeId: probeId,
                    geneSymbol: bioMarker.bioMarkerName,
                    geneId: bioMarker.primaryExternalId,
                    organism: 'Homo sapiens',
            )
            res.id = probesetId
            res
        }
        [
                createAnnotation(-201, '1553506_at', bioMarkers[0]),
                createAnnotation(-202, '1553510_s_at', bioMarkers[1]),
                createAnnotation(-203, '1553510_s_at', bioMarkers[2]),
        ]
    }()

    static List<PatientDimension> patients =
        HighDimTestData.createTestPatients(2, -300, TRIAL_NAME)

    static List<DeSubjectSampleMapping> assays =
        HighDimTestData.createTestAssays(patients, -400, platform, TRIAL_NAME)

    static List<DeSubjectMicroarrayDataCoreDb> microarrayData = {
        def common = [
                trialName: TRIAL_NAME,
                //trialSource: "$TRIAL_NAME:STD" (not mapped)
        ]
        def createMicroarrayEntry = { DeSubjectSampleMapping assay,
                                      DeMrnaAnnotationCoreDb probe,
                                      double intensity ->
            new DeSubjectMicroarrayDataCoreDb(
                    probe: probe,
                    assay: assay,
                    patient: assay.patient,
                    rawIntensity: intensity,
                    logIntensity: Math.log(intensity) / Math.log(2),
                    zscore: Math.random() * 4 - 2, /* non-sensical value */
                    *: common,
            )
        }

        def res = []
        Double intensity = 0
        annotations.each { probe ->
            assays.each { assay ->
                res += createMicroarrayEntry assay, probe, (intensity += 0.1)
            }
        }

        res
    }()


    static void saveAll() {
        assertThat platform.save(), is(notNullValue(DeGplInfo))
        save bioMarkers
        save principals
        save users
        save geneSignatures
        save assayFeatureGroups
        save assayAnnotations
        save geneSignatureItems
        save searchKeywords
        save annotations
        save patients
        save assays
        save microarrayData
    }
}
