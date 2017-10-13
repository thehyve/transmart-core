package org.transmartproject.solr

import com.google.common.collect.ImmutableMultimap
import grails.transaction.Rollback
import grails.test.mixin.integration.Integration
import org.apache.solr.client.solrj.SolrQuery
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.solr.modules.BrowseAssaysIndexingModule
import org.transmartproject.solr.modules.FilesIndexingModule
import spock.lang.Specification

@Integration
@Rollback
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


    void clearIndex() {
        facetsIndexingService.clearIndex()
        facetsQueryingService.clearCaches()
    }

    void reindex() {
        facetsIndexingService.clearIndex()
        facetsIndexingService.fullIndex()
        facetsQueryingService.clearCaches()
    }

    void 'test database initialisation' () {
        when:
        List obfs = ObservationFact.all

        then:
        assert obfs.size() > 0
    }

    void 'testGetAllDisplaySettings' () {
        given:
        reindex()

        when:
        def allSettings = facetsQueryingService.allDisplaySettings
        def firstEntry = allSettings.entrySet()[0]

        then:
        //allSettings.size() == 2
        firstEntry.key == 'TEXT'
        firstEntry.value.displayName == 'Full Text'
        !firstEntry.value.hideFromListings
    }

    void 'testGetTopTerms' () {
        given:
        reindex()

        when:
        def topTerms = facetsQueryingService.getTopTerms('CONCEPT_PATH')

        then:
        //order by count, then by name
        assert topTerms.size() > 0
    }

    private int countDocuments(String obj) {
        SolrQuery q = new SolrQuery(obj)
        q.set('rows', 0)
        solrFacetsCore.query(q).results.numFound
    }

}
