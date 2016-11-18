package org.transmartproject.search.indexing

import com.google.common.collect.Sets
import grails.test.mixin.TestMixin
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin
import org.transmartproject.search.indexing.modules.*

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.search.indexing.FacetsDocumentMatcher.documentWithFields
import static org.transmartproject.search.indexing.modules.AbstractFacetsIndexingFolderModule.FOLDER_DOC_TYPE
import static org.transmartproject.search.indexing.modules.FilesIndexingModule.FILE_DOC_TYPE

@TestMixin(RuleBasedIntegrationTestMixin)
class BrowseIndexingModulesTests {

    @Autowired
    BrowseAssaysIndexingModule browseAssaysIndexingModule

    @Autowired
    BrowsePlainFoldersIndexingModule browsePlainFoldersIndexingModule

    @Autowired
    BrowseStudiesIndexingModule browseStudiesIndexingModule

    @Autowired
    BrowseProgramsIndexingModule browseProgramsIndexingModule

    @Autowired
    BrowseTagsIndexingModule browseTagsIndexingModule

    @Autowired
    FilesIndexingModule filesIndexingModule

    @Test
    void testAssays() {
        def docs = browseAssaysIndexingModule
                .collectDocumentsWithIds(Sets.newHashSet(browseAssaysIndexingModule.fetchAllIds(FOLDER_DOC_TYPE)))

        assert docs.size() == 1
        assertThat docs, contains(
                documentWithFields('FOLDER:1992449',
                        ['title_t', 'GSE8581 Assay'],
                        ['description_t', 'GSE8581 Assay'],
                        ['measurement_type_s', 'Transcription Profiling'],
                        ['platform_s', 'Agilent-011521 Human 1A Microarray G4110A  (Feature Number version)'],
                        ['vendor_s', 'Agilent Technologies'],
                        ['technology_s', 'DNA Microarray'],
                        ['gene_s', 'TP53'],
                        ['TEXT', 'tumor protein p53'],
                        ['mirna_s', 'TP53'],
                        ['biomarker_type_s', 'DNA'],
                        ['TEXT', 'DNA'],
                ))
    }

    @Test
    void testFolders() {
        def docs = browsePlainFoldersIndexingModule
                .collectDocumentsWithIds(Sets.newHashSet(browsePlainFoldersIndexingModule.fetchAllIds(FOLDER_DOC_TYPE)))

        assert docs.size() == 3
        docs.sort(true)

        assertThat docs, contains(
                documentWithFields('FOLDER:1992457',
                        ['title_t', 'Raw data'],
                        ['description_t', 'Raw'],
                        ['file_type_s', 'ETL files'],
                        ['TEXT', 'ETL files'],
                ),
                documentWithFields('FOLDER:1992458',
                        ['title_t', 'Data'],
                        ['description_t', 'Data'],
                        ['file_type_s', 'ETL files'],
                        ['TEXT', 'ETL files'],
                ),
                documentWithFields('FOLDER:1992458',
                        ['title_t', 'Test folder'],
                        ['description_t', 'yes'],
                        ['file_type_s', 'Report'],
                        ['TEXT', 'Report'],
                ),
        )
    }

    @Test
    void testOneStudy() {
        def doc = browseStudiesIndexingModule.collectDocumentsWithIds([new FacetsDocId('FOLDER:1992454')] as Set)
        assertThat doc.first(), is(documentWithFields('FOLDER:1992454',
                ['title_t'          , 'Cell-line']                           ,
                ['description_t'    , 'Cell-line']                           ,
                ['design_s'         , 'Observational']                       ,
                ['TEXT'             , 'Observational']                       ,
                ['biomarker_type_s' , 'Efficacy biomarker']                  ,
                ['TEXT'             , 'Efficacy biomarker']                  ,
                ['access_type_s'    , 'Consortium']                          ,
                ['TEXT'             , 'Consortium']                          ,
                ['accession_s'      , 'Cell-line']                           ,
                ['country_s'        , 'NETHERLANDS']                         ,
                ['TEXT'             , 'NETHERLANDS']                         ,
                ['disease_s'        , 'Colonic Neoplasms']                   ,
                ['disease_s'        , 'Prostatic Neoplasms']                 ,
                ['study_objective_s', 'Discover targets/biological contexts'],
                ['TEXT'             , 'Discover targets/biological contexts'],
                ['species_s'        , 'Homo Sapiens']                        ,
                ['TEXT'             , 'Homo sapiens']                        ,
                ['study_phase_s'    , 'Not applicable']                      ,
                ['TEXT'             , 'Not applicable']                      ,
        ))
    }

    @Test
    void testOneProgram() {
        def doc = browseProgramsIndexingModule.collectDocumentsWithIds([new FacetsDocId('FOLDER:1992447')] as Set)
        assertThat doc.first(), is(documentWithFields('FOLDER:1992447',
                ['title_t'             , 'Public Studies'],
                ['description_t'       , 'Public Studies'],
                ['disease_s'           , 'Ego']           ,
                ['therapeutic_domain_s', 'Other']         ,
                ['TEXT'                , 'Other']         ,
        ))
    }

    @Test
    void testTagsForOneFolder() {
        def doc = browseTagsIndexingModule.collectDocumentsWithIds([new FacetsDocId('FOLDER:1992450')] as Set)
        assertThat doc.first(), is(documentWithFields('FOLDER:1992450',
                ['tag_study_link_t', 'RA_BADOT_GSE15602'],
                ['tag_number_of_followed_subjects_i', 100L],
                ['tag_study_date_t', '1-1-2000'],
                ['tag_study_pubmed_id_t', 'RA_Badot_GSE15602'],
                ['tag_study_publication_study_publication_doi_t', 'RA_Badot_GSE15602'],
                ['tag_study_publication_study_publication_author_list_t', 'RA_Badot_GSE15602'],
                ['tag_study_publication_study_publication_title_t', 'RA_Badot_GSE15602'],
        ))
    }

    @Test
    void testFindSpecificFile() {
        def doc = filesIndexingModule.collectDocumentsWithIds([new FacetsDocId('FILE:1992460')] as Set)
        assert doc.size() == 1
        assert doc[0].facetsDocId == new FacetsDocId('FILE:1992460')
    }

    @Test
    void testBrowseTagsFieldDisplaySettings() {
        def res = browseTagsIndexingModule.getDisplaySettingsForIndex('tag_study_pubmed_id_t')
        assert res.displayName == 'Study PubMed ID'
        assert res.order == 1018 /* 1000 + display order */
    }
}
