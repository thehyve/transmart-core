package org.transmartproject.db.dataquery.highdim

import org.transmartproject.db.bioassay.BioAssayDataAnnotationCoreDb
import org.transmartproject.db.bioassay.BioAssayFeatureGroupCoreDb
import org.transmartproject.db.biomarker.BioDataCorrelationCoreDb
import org.transmartproject.db.biomarker.BioMarkerCoreDb
import org.transmartproject.db.search.SearchGeneSignature
import org.transmartproject.db.search.SearchGeneSignatureItem
import org.transmartproject.db.search.SearchKeywordCoreDb
import org.transmartproject.db.user.SearchAuthPrincipal
import org.transmartproject.db.user.SearchAuthUser

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.createBioMarkers
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.createCorrelationPairs
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.createSearchKeywordsForBioMarkers
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.createSearchKeywordsForGeneSignatures
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

class SampleBioMarkerTestData {

    List<BioMarkerCoreDb> geneBioMarkers = createBioMarkers(-1100L, [
            [ name: 'BOGUSCPO',
                    description: 'carboxypeptidase O',
                    primaryExternalId: '-130749' ],
            [ name: 'BOGUSRQCD1',
                    description: 'RCD1 required for cell differentiation1 homolog (S. pombe)',
                    primaryExternalId: '-9125' ],
            [ name: 'BOGUSVNN3',
                    description: 'vanin 3',
                    primaryExternalId: '-55350' ],
            [ name: 'BOGUSCPOCORREL',
                    description: 'Bogus gene associated with BOGUSCPO',
                    primaryExternalId: '-130750']])

    List<BioMarkerCoreDb> proteinBioMarkers = createBioMarkers(-1200L, [
            [ name: 'BOGUSCBPO_HUMAN',
                    description: 'Carboxypeptidase O',
                    primaryExternalId: 'BOGUS_Q8IVL8' ]],
            'PROTEIN',
            'HOMO SAPIENS',
            'UniProt')

    List<BioMarkerCoreDb> mirnaBioMarkers = createBioMarkers(-1400L, [
            [ name: 'MIR3161',
                    description: 'Homo sapiens miR-3161 stem-loop',
                    primaryExternalId: 'hsa-mir-3161' ],
            [ name: 'MIR1260B',
                    description: 'Homo sapiens miR-1260b stem-loop',
                    primaryExternalId: 'hsa-mir-1260b' ],
            [ name: 'MIR323B',
                    description: 'Homo sapiens miR-323b stem-loop',
                    primaryExternalId: 'hsa-mir-323b' ]],
            'MIRNA',
            'HOMO SAPIENS',
            'miRBase')


    List<SearchKeywordCoreDb> geneSearchKeywords =
        createSearchKeywordsForBioMarkers(geneBioMarkers, -2100L)

    List<SearchKeywordCoreDb> proteinSearchKeywords =
        createSearchKeywordsForBioMarkers(proteinBioMarkers, -2200L)

    List<SearchKeywordCoreDb> mirnaSearchKeywords =
        createSearchKeywordsForBioMarkers(mirnaBioMarkers, -2400L)

    List<BioDataCorrelationCoreDb> geneCorrelations = createCorrelationPairs(-3100L,
            [ geneBioMarkers.find { it.name == 'BOGUSCPOCORREL' } ], /* from */
            [ geneBioMarkers.find { it.name ==  'BOGUSCPO' } ]       /* to */)

    List<BioDataCorrelationCoreDb> proteinGeneCorrelations = createCorrelationPairs(-3200L,
            [ proteinBioMarkers.find { it.name == 'BOGUSCBPO_HUMAN' } ],
            [ geneBioMarkers.find { it.name ==  'BOGUSCPO' } ])


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
     *   item -901 -> bioMarker -1101 (BOGUSCPO)
     *   item -902 -> bioMarker -1102 (BOGUSRQCD1)
     *   item -901 -> probeSet -701 -> annotation #0 -> bioMarker -1102 (BOGUSRQCD1)
     *   item -902 -> probeSet -702 -> annotation #1 -> bioMarker -1101 (BOGUSCPO)
     *
     * Gene signature -602:
     *   item -903 -> bioMarker -1103 (BOGUSVNN3)
     *   item -903 -> probeSet -701 -> annotation #0 -> bioMarker -1102 (BOGUSRQCD1)
     */
    List<SearchAuthPrincipal> principals = {
        def res = [
                new SearchAuthPrincipal(enabled: true)
        ]
        res[0].id = -1001
        res
    }()

    List<SearchAuthUser> users = {
        def res = [
                new SearchAuthUser(username: 'foobar')
        ]
        res[0].id = principals[0].id
        res
    }()

    List<SearchGeneSignature> geneSignatures = {
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

    List<BioAssayFeatureGroupCoreDb> assayFeatureGroups = {
        (-702..-701).reverse().collect {
            def res = new BioAssayFeatureGroupCoreDb(
                    name: 'probeSet' + it,
                    type: 'foobar'
            )
            res.id = it
            res
        }
    }()

    List<BioAssayDataAnnotationCoreDb> assayAnnotations = {
        [
                new BioAssayDataAnnotationCoreDb(
                        probeSet: assayFeatureGroups[0],
                        bioMarker: geneBioMarkers[1],
                ),
                new BioAssayDataAnnotationCoreDb(
                        probeSet: assayFeatureGroups[1],
                        bioMarker: geneBioMarkers[0],
                ),
        ]
    }()

    List<SearchGeneSignatureItem> geneSignatureItems = {
        def createGeneSignatureItem = { BioMarkerCoreDb bioMarker,
                                        SearchGeneSignature geneSignature,
                                        Long foldChangeMetric,
                                        BioAssayFeatureGroupCoreDb probeSet,
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
                createGeneSignatureItem(geneBioMarkers[0], geneSignatures[0], -1L, assayFeatureGroups[0], -901),
                createGeneSignatureItem(geneBioMarkers[1], geneSignatures[0], 0L,  assayFeatureGroups[1], -902),
                createGeneSignatureItem(geneBioMarkers[2], geneSignatures[1], 1L,  assayFeatureGroups[0], -903),
        ]
    }()

    List<SearchKeywordCoreDb> geneSignatureSearchKeywords =
        createSearchKeywordsForGeneSignatures(geneSignatures, -2300L)


    void saveGeneData() {
        save geneBioMarkers
        save geneSearchKeywords
        save geneCorrelations

        save proteinBioMarkers
        save proteinSearchKeywords
        save proteinGeneCorrelations

        save principals
        save users
        save geneSignatures
        save assayFeatureGroups
        save assayAnnotations
        save geneSignatureItems
        save geneSignatureSearchKeywords
    }

    void saveMirnaData() {
        save mirnaBioMarkers
        save mirnaSearchKeywords
    }
}
