package transmart.solr.indexing

import com.google.common.collect.ImmutableMultimap
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.Integration
import grails.transaction.Transactional
import org.apache.solr.client.solrj.SolrQuery
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Ignore
import spock.lang.IgnoreRest
import spock.lang.Specification

import static transmart.solr.indexing.FacetsIndexingService.FIELD_NAME_CONCEPT_PATH
import static transmart.solr.indexing.FacetsIndexingService.FIELD_NAME_FOLDER_ID

@Ignore
@Integration
@Transactional
class FacetsIndexingServiceSpec extends Specification{

    public static final int NUMBER_OF_FOLDERS = 13
    public static final int NUMBER_OF_FILES = 1
    public static final int NUMBER_OF_CONCEPTS = 373 + 1 /* 338 in i2b2 + 'Across Trials' */
    public static final int TOTAL_NUMBER_OF_DOCUMENTS = NUMBER_OF_FOLDERS + NUMBER_OF_FILES + NUMBER_OF_CONCEPTS
    public static final int NUMBER_OF_FILE_DOCUMENTS = 1

    @Autowired
    SolrFacetsCore solrFacetsCore

    @Autowired
    FacetsIndexingService indexingService
//
//    @Rule
//    public TestRule skipIfSolrNotAvailableRule = new SkipIfSolrNotAvailableRule()

    @Before
    void before() {
        indexingService.clearIndex()
    }

    @Test
    //@IgnoreRest
    void 'testFullIndex' () {
        when:
        indexingService.fullIndex()

        then:
        assert countDocuments('*:*') == TOTAL_NUMBER_OF_DOCUMENTS
    }

    @Test
    void 'testIndexByTypes' () {
        when:
        indexingService.indexByTypes(['FILE'] as Set)

        then:
        assert countDocuments('*:*') == NUMBER_OF_FILE_DOCUMENTS
        assert countDocuments('TYPE:FILE') == NUMBER_OF_FILE_DOCUMENTS
    }

    @Test
    void 'testIndexByIds' () {
        when:
        indexingService.indexByIds([new FacetsDocId('FOLDER:1992454')] as Set)

        then:
        assert countDocuments('*:*') == 1
        assert countDocuments('SUBTYPE:STUDY') == 1
    }

    //@ignore
    @Test
    void 'testStuffIsMergedFromDifferentModules' () {
        when:
        String id = 'FOLDER:1992454'
        indexingService.indexByIds([new FacetsDocId(id)] as Set)

        then:
        assert countDocuments("id:\"$id\" AND tag_number_of_followed_subjects_i:19 AND design_s:Observational") == 1
    }

    //@ignore
    @Test
    void 'testConceptPathLinkIsCorrect' () {
        when:
        def expectedMappings = [
                'FOLDER:1992449': '\\Public Studies\\GSE8581\\',
                'FOLDER:1992454': '\\Public Studies\\Cell-line\\',]
        indexingService.indexByIds(
                expectedMappings.keySet().collect { new FacetsDocId(it) } as Set)

        def q = new SolrQuery('*:*')
        q.sort = new SolrQuery.SortClause('_docid_', 'asc')
        def results = solrFacetsCore.query(q).results

        then:
        assert results[0][FIELD_NAME_CONCEPT_PATH] == expectedMappings['FOLDER:1992449']
        assert results[1][FIELD_NAME_CONCEPT_PATH] == expectedMappings['FOLDER:1992454']
    }

    //@ignore
    @Test
    void 'testFoldersHaveCorrectConcepts' () {
        when:
        def expectedMappings = [
                '\\Public Studies\\GSE8581\\': 1992448L,
                '\\Public Studies\\GSE8581\\Endpoints\\FEV1\\': 1992448L,
                '\\Public Studies\\phenotypedata\\': 1992451L,
        ]
        indexingService.indexByIds(
                expectedMappings.keySet().collect { new FacetsDocId('CONCEPT', it) } as Set)

        def q = new SolrQuery('*:*')
        q.sort = new SolrQuery.SortClause(FIELD_NAME_CONCEPT_PATH, 'asc')
        def results = solrFacetsCore.query(q).results

        then:
        assert results[0][FIELD_NAME_FOLDER_ID] == expectedMappings['\\Public Studies\\GSE8581\\']
        assert results[1][FIELD_NAME_FOLDER_ID] == expectedMappings['\\Public Studies\\GSE8581\\Endpoints\\FEV1\\']
        assert results[2][FIELD_NAME_FOLDER_ID] == expectedMappings['\\Public Studies\\phenotypedata\\']
    }

    //@ignore
    @Test
    void 'unamanagedTagsAreIndexed' () {
        when:
        def fullName = '\\Public Studies\\GSE13732\\Biomarker Data\\GPL570\\Blood\\Replication 1\\Baseline\\'
        indexingService.indexByIds([new FacetsDocId('CONCEPT', fullName) ] as Set)

        then:
        assert countDocuments('TEXT:expression AND TEXT:profiling') == 1
    }

    //@ignore
    @Test
    void 'testManagedTagsAreIndexedWithFolder' () {
        when:
        indexingService.indexByIds([
                new FacetsDocId('FOLDER:1992448'),
                new FacetsDocId('FOLDER:1992451')] as Set)

        then:
        assert countDocuments('test_analyzed_tag_t:Option') == 1
        assert countDocuments('programming_language_s:*') == 2
    }

    //@ignore
    @Test
    void 'testManagedTagsAreIndexedWithoutFolderLink' () {
        when:
        indexingService.indexByIds([
                new FacetsDocId('CONCEPT:\\Public Studies\\GSE13732\\'),] as Set)

        then:
        assert countDocuments('programming_language_s:Python AND id:CONCEPT\\:*') == 1
    }

    //@ignore
    @Test
    void 'testDataTypesAreIndexedWithFolder' () {
        when:
        indexingService.indexByIds([
                new FacetsDocId('FOLDER:1992448'),] as Set)

        then:
        assert countDocuments('data_type_s:"Messenger RNA data (Microarray)" AND id:FOLDER\\:1992448') == 1
    }

    //@ignore
    @Test
    void 'testDataTypesAreIndexedWithoutFolderLink' () {
        when:
        indexingService.indexByIds([
                new FacetsDocId('CONCEPT:\\Public Studies\\GSE13732\\'),] as Set)

        then:
        assert countDocuments('data_type_s:"Messenger RNA data (Microarray)" AND id:CONCEPT\\:*') == 1
    }

    //@ignore
    @Test
    void 'addDocument' () {
        when:
        String id = 'FOO:12345'

        indexingService.addDocument(new FacetsDocument(
                facetsDocId: new FacetsDocId(id),
                fieldValues: ImmutableMultimap.of(),
        ))
        indexingService.flush()

        then:
        assert countDocuments("id:\"$id\"") == 1
    }

    //@ignore
    @Test
    void 'testRemoveDocument' () {
        when:
        def id = new FacetsDocId('FOO:12345')

        indexingService.addDocument(new FacetsDocument(
                facetsDocId: id,
                fieldValues: ImmutableMultimap.of(),
        ))
        indexingService.flush()

        indexingService.removeDocuments([id] as Set)
        indexingService.flush()

        then:
        assert countDocuments('id:"FOO:12345"') == 0
    }


    private int countDocuments(obj) {
        SolrQuery q = new SolrQuery(obj)
        q.set('rows', 0)
        solrFacetsCore.query(q).results.numFound
    }
}
