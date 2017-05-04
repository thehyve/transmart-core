package transmart.solr.indexing

import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Sets
import grails.test.mixin.integration.Integration
import grails.transaction.Transactional
import org.apache.solr.client.solrj.SolrQuery
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Test
import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.i2b2data.ObservationFact
import spock.lang.Specification
import transmart.solr.indexing.BrowseStudiesView
import transmart.solr.indexing.FacetsDocId
import transmart.solr.indexing.FacetsDocument
import transmart.solr.indexing.FacetsIndexingService
import transmart.solr.indexing.FacetsQueryingService
import transmart.solr.indexing.FolderStudyMappingView
import transmart.solr.indexing.SolrFacetsCore
import transmart.solr.indexing.modules.BrowseAssaysIndexingModule
import transmart.solr.indexing.modules.FilesIndexingModule

import static org.hamcrest.MatcherAssert.assertThat
import static transmart.solr.indexing.modules.AbstractFacetsIndexingFolderModule.FOLDER_DOC_TYPE
import grails.test.mixin.integration.Integration
import grails.transaction.Transactional
import org.apache.solr.client.solrj.SolrQuery
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.i2b2data.ObservationFact
import spock.lang.Specification

/**
 * Created by jarno@thehyve.nl
 */

@Integration
@Transactional
class TestSetupSpec extends Specification {

    @Autowired
    FacetsIndexingService facetsIndexingService

    @Autowired
    SolrFacetsCore solrFacetsCore

    @Autowired
    FacetsQueryingService facetsQueryingService

    @Autowired
    BrowseAssaysIndexingModule browseAssaysIndexingModule

    @Autowired
    FilesIndexingModule filesIndexingModule

    @Before
    void before() {
        facetsIndexingService.fullIndex()
    }

//    @After
//    void after () {
//        facetsIndexingService.clearIndex()
//    }

    @Test
    void 'test h2 connection' () {
        when:
        List obfs = ObservationFact.all

        then:
        assert obfs.size() == 4
        facetsIndexingService.fullIndex()
        assert 'hoi' == 'hoi'
    }

    @Test
    void 'test add solr info folderstudymapping' () {
        when:
        FolderStudyMappingView f = new FolderStudyMappingView([root:false, conceptPath: "\\\\Nikskkk\\hoi\\", folderId: 100, id: "Test123"])
        f.save()
        facetsIndexingService.indexByIds([
                new FacetsDocId('CONCEPT:\\Niks\\hoi\\'),] as Set)
        facetsIndexingService.flush()

        then:
        assert FolderStudyMappingView.all.size() == 1
    }

    @Test
    void 'test add browse studies' () {
        when:
        BrowseStudiesView b = new BrowseStudiesView(id: "test", title: "test", description: "this is a test",
                design: "niemand", biomarkerType: "voet", accessType: "iedereen", accession: "noppe",
                institution: "the hyve", country: "The Netherlands", disease: "kroketten", compound: "vlees",
        studyObjective: "niets", organism: "Human", studyPhase: "einde")
        b.save()
        facetsIndexingService.fullIndex()
        facetsIndexingService.flush()

        then:
        assert BrowseStudiesView.all.size() == 1

    }

    @Test
    void 'addDocument' () {
        when:
        String id = 'FOO:12345'

        facetsIndexingService.addDocument(new FacetsDocument(
                facetsDocId: new FacetsDocId(id),
                fieldValues: ImmutableMultimap.of(),
        ))
        facetsIndexingService.flush()

        then:
        assert countDocuments("id:\"$id\"") == 1
    }

    @Test
    void 'testRemoveDocument' () {
        when:
        def id = new FacetsDocId('FOO:12345')

        facetsIndexingService.addDocument(new FacetsDocument(
                facetsDocId: id,
                fieldValues: ImmutableMultimap.of(),
        ))
        facetsIndexingService.flush()

        facetsIndexingService.removeDocuments([id] as Set)
        facetsIndexingService.flush()

        then:
        assert countDocuments('id:"FOO:12345"') == 0
    }

    @Test
    void 'testGetAllDisplaySettings' () {
        when:
        def allSettings = facetsQueryingService.allDisplaySettings
        def firstEntry = allSettings.entrySet()[0]

        then:
        //assert allSettings.size() == 2
        assert firstEntry.key == 'TEXT'
        assert firstEntry.value.displayName == 'Full Text'
        assert !firstEntry.value.hideFromListings
    }

    @Test
    void 'testGetTopTerms' () {
        when:
        def topTerms = facetsQueryingService.getTopTerms("CONCEPT")

        then:
        //order by count, then by name
        assert topTerms.size() > 0
    }

    @Test
    void 'testAssays' () {
        when:
        def docs = browseAssaysIndexingModule
                .collectDocumentsWithIds(Sets.newHashSet(browseAssaysIndexingModule.fetchAllIds(FOLDER_DOC_TYPE)))

        then:
        assert docs.size() == 1
//        assertThat docs, Matchers.contains(
//                FacetsDocumentMatcher.documentWithFields('FOLDER:1992449',
//                        ['title_t', 'GSE8581 Assay'],
//                        ['description_t', 'GSE8581 Assay'],
//                        ['measurement_type_s', 'Transcription Profiling'],
//                        ['platform_s', 'Agilent-011521 Human 1A Microarray G4110A  (Feature Number version)'],
//                        ['vendor_s', 'Agilent Technologies'],
//                        ['technology_s', 'DNA Microarray'],
//                        ['gene_s', 'TP53'],
//                        ['TEXT', 'tumor protein p53'],
//                        ['mirna_s', 'TP53'],
//                        ['biomarker_type_s', 'DNA'],
//                        ['TEXT', 'DNA'],
//                ))
    }

    private int countDocuments(obj) {
        SolrQuery q = new SolrQuery(obj)
        q.set('rows', 0)
        solrFacetsCore.query(q).results.numFound
    }

}
