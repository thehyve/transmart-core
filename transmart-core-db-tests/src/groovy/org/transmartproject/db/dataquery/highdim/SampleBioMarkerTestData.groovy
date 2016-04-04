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

import org.transmartproject.db.bioassay.BioAssayDataAnnotationCoreDb
import org.transmartproject.db.bioassay.BioAssayFeatureGroupCoreDb
import org.transmartproject.db.biomarker.BioDataCorrelationCoreDb
import org.transmartproject.db.biomarker.BioMarkerCoreDb
import org.transmartproject.db.search.SearchGeneSignature
import org.transmartproject.db.search.SearchGeneSignatureItem
import org.transmartproject.db.search.SearchKeywordCoreDb
import org.transmartproject.db.user.User

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.*

class SampleBioMarkerTestData {

    List<BioMarkerCoreDb> geneBioMarkers = createBioMarkers(-1100L, [
            [ name: 'BOGUSCPO',
                    description: 'carboxypeptidase O',
                    externalId: '-130749' ],
            [ name: 'BOGUSRQCD1',
                    description: 'RCD1 required for cell differentiation1 homolog (S. pombe)',
                    externalId: '-9125' ],
            [ name: 'BOGUSVNN3',
                    description: 'vanin 3',
                    externalId: '-55350' ],
            [ name: 'BOGUSCPOCORREL',
                    description: 'Bogus gene associated with BOGUSCPO',
                    externalId: '-130750'],
            [ name: 'AURKA',
                    description: 'Related with Adiponectin antigene',
                    externalId: '-130751'],
            [ name: 'SLC14A2',
                    description: 'Related with Urea transporter 2 antigen',
                    externalId: '-130752'],
            [ name: 'ADIRF',
                    description: 'Related with Adipogenesis regulatory factor',
                    externalId: '-130753']])

    List<BioMarkerCoreDb> proteinBioMarkers = createBioMarkers(-1200L, [
            [ name: 'BOGUSCBPO_HUMAN',
                    description: 'Carboxypeptidase O',
                    externalId: 'BOGUS_Q8IVL8' ],
            [ name: 'Adipogenesis regulatory factor',
                    description: 'Adipogenesis factor rich in obesity',
                    externalId: 'Q15847' ],
            [ name: 'Adiponectin',
                    description: '30 kDa adipocyte complement-related protein',
                    externalId: 'Q15848' ],
            [ name: 'Urea transporter 2',
                    description: 'Solute carrier family 14 member 2',
                    externalId: 'Q15849' ],
            [ name: 'EMBL CAA39792.1',
                    description: 'EMBL CAA39792.1',
                    externalId: 'Q15850' ]],
            'PROTEIN',
            'HOMO SAPIENS',
            'UniProt')

    List<BioMarkerCoreDb> mirnaBioMarkers = createBioMarkers(-1400L, [
            [ name: 'MIR3161',
                    description: 'Homo sapiens miR-3161 stem-loop',
                    externalId: 'hsa-mir-3161' ],
            [ name: 'MIR1260B',
                    description: 'Homo sapiens miR-1260b stem-loop',
                    externalId: 'hsa-mir-1260b' ],
            [ name: 'MIR323B',
                    description: 'Homo sapiens miR-323b stem-loop',
                    externalId: 'hsa-mir-323b' ]],
            'MIRNA',
            'HOMO SAPIENS',
            'miRBase')

    List<BioMarkerCoreDb> pathwayBioMarkers = createBioMarkers(-1500L, [
            [ name: 'FOOPATHWAY',
                    description: 'Foo pathway',
                    externalId: 'foo_pathway']],
            'PATHWAY',
            'HOMO SAPIENS',
            'foo')

    /* keep in sync with MetaboliteTestData::annotations */
    List<BioMarkerCoreDb> metaboliteBioMarkers = createBioMarkers(-1600L, [
            [ name: 'HMDB30536',
                    description: 'Majoroside F4',
                    externalId: 'HMDB30536'],
            [ name: 'HMDB30537',
                    description: 'Cryptoxanthin 5,6:5\',8\'-diepoxide',
                    externalId: 'HMDB30537'],
            [ name: 'HMDB30538',
                    description: 'Cryptoxanthin epoxide',
                    externalId: 'HMDB30538']],
            'METABOLITE',
            'HOMO SAPIENS',
            'HMDB')

    List<SearchKeywordCoreDb> geneSearchKeywords =
        createSearchKeywordsForBioMarkers(geneBioMarkers, -2100L)

    List<SearchKeywordCoreDb> proteinSearchKeywords =
        createSearchKeywordsForBioMarkers(proteinBioMarkers, -2200L)

    List<SearchKeywordCoreDb> mirnaSearchKeywords =
        createSearchKeywordsForBioMarkers(mirnaBioMarkers, -2400L)

    List<SearchKeywordCoreDb> pathwaySearchKeywords =
        createSearchKeywordsForBioMarkers(pathwayBioMarkers, -2500L)

    List<SearchKeywordCoreDb> metaboliteSearchKeywords =
        createSearchKeywordsForBioMarkers(metaboliteBioMarkers, -2600L)


    @Lazy  // Lazy so other test data can be accessed outside of a grails application context, e.g. in an APIClient
    List<BioDataCorrelationCoreDb> geneCorrelations = { createCorrelationPairs(-3100L,
            [ geneBioMarkers.find { it.name == 'BOGUSCPOCORREL' } ], /* from */
            [ geneBioMarkers.find { it.name ==  'BOGUSCPO' } ]       /* to */) }()

    @Lazy
    List<BioDataCorrelationCoreDb> proteinGeneCorrelations = { createCorrelationPairs(-3200L,
            [ proteinBioMarkers.find { it.name == 'BOGUSCBPO_HUMAN' } ],
            [ geneBioMarkers.find { it.name ==  'BOGUSCPO' } ]) }()

    @Lazy
    List<BioDataCorrelationCoreDb> geneProteinCorrelations = { createCorrelationPairs(-3300L,
            [
                    geneBioMarkers.find { it.name ==  'AURKA' },
                    geneBioMarkers.find { it.name ==  'SLC14A2' },
                    geneBioMarkers.find { it.name ==  'ADIRF' },
            ],
            [
                    proteinBioMarkers.find { it.name == 'Adiponectin' },
                    proteinBioMarkers.find { it.name == 'Urea transporter 2' },
                    proteinBioMarkers.find { it.name == 'Adipogenesis regulatory factor' },
            ]) }()

    @Lazy
    List<BioDataCorrelationCoreDb> pathwayGeneCorrelation = { createCorrelationPairs(-3400L,
            [
                    pathwayBioMarkers.find { it.name == 'FOOPATHWAY'},
            ],
            [
                    geneBioMarkers.find { it.name == 'AURKA'}
            ]) }()

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
    List<User> users = {
        def res = [
                new User(username: 'foobar')
        ]
        res[0].id = -1001L
        res[0].enabled = true
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
                    speciesConceptId: 0,
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

    List<BioMarkerCoreDb> getAllBioMarkers() {
        [geneBioMarkers, proteinBioMarkers, pathwayBioMarkers, mirnaBioMarkers, metaboliteBioMarkers].flatten()
    }

    void saveGeneData() {
        save geneBioMarkers
        save geneSearchKeywords
        save geneCorrelations

        save proteinBioMarkers
        save proteinSearchKeywords

        save proteinGeneCorrelations

        save users
        save geneSignatures
        save assayFeatureGroups
        save assayAnnotations
        save geneSignatureItems
        save geneSignatureSearchKeywords
    }

    void saveProteinData() {
        save geneBioMarkers
        save geneSearchKeywords

        save proteinBioMarkers
        save proteinSearchKeywords

        save pathwayBioMarkers
        save pathwaySearchKeywords

        save pathwayGeneCorrelation
        save geneProteinCorrelations
    }

    void saveMirnaData() {
        save mirnaBioMarkers
        save mirnaSearchKeywords
    }

    void saveMetabolomicsData() {
        save metaboliteBioMarkers
        save metaboliteSearchKeywords
    }

    void saveAll() {
        saveGeneData()
        saveProteinData()
        saveMirnaData()
        saveMetabolomicsData()
    }
}
