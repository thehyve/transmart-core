package org.transmartproject.solr

import com.google.common.collect.ImmutableMultimap
import grails.test.mixin.integration.Integration
import grails.transaction.Transactional
import org.apache.solr.client.solrj.SolrQuery
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

import static FacetsIndexingService.FIELD_NAME_CONCEPT_PATH
import static FacetsIndexingService.FIELD_NAME_FOLDER_ID

/**
 * This test class assumes the test data and the GSE8581 to be loaded:
 * - transmart-data: source vars && make postgres_test
 * - transmart-batch: ./gradlew loadPublicTestStudy
 */
@Integration
@Transactional
class FacetsIndexingServiceSpec extends Specification {

    public static final int NUMBER_OF_CONCEPTS = 234
    public static final int TOTAL_NUMBER_OF_DOCUMENTS = NUMBER_OF_CONCEPTS

    @Autowired
    SolrFacetsCore solrFacetsCore

    @Autowired
    FacetsIndexingService indexingService

    void clearIndex() {
        indexingService.clearIndex()
    }

    void 'testFullIndex' () {
        given:
        clearIndex()

        when:
        indexingService.fullIndex()

        then:
        assert countDocuments('*:*') == TOTAL_NUMBER_OF_DOCUMENTS
    }

    void 'testTagsAreIndexed' () {
        given:
        clearIndex()

        when:
        indexingService.fullIndex()

        then:
        assert countDocuments('test_tag_s:*') == 1
        assert countDocuments('TEXT:Human Chronic Obstructive Pulmonary Disorder') == 2
    }

    void 'testDataTypesAreIndexed' () {
        given:
        clearIndex()

        when:
        indexingService.fullIndex()

        then:
        assert countDocuments('data_type_s:"Messenger RNA data (Microarray)"') == 8
    }

    void 'test add document' () {
        given:
        clearIndex()

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

    void 'test remove document' () {
        given:
        clearIndex()

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


    private int countDocuments(String obj) {
        SolrQuery q = new SolrQuery(obj)
        q.set('rows', 0)
        solrFacetsCore.query(q).results.numFound
    }
}
