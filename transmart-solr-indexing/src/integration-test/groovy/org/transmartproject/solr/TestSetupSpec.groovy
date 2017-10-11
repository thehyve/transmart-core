package org.transmartproject.solr

import com.google.common.collect.ImmutableMultimap
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.apache.solr.client.solrj.SolrQuery
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.solr.modules.BrowseAssaysIndexingModule
import org.transmartproject.solr.modules.FilesIndexingModule
import spock.lang.Specification

/**
 * Created by jarno@thehyve.nl
 */

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

    def setup() {
        facetsIndexingService.fullIndex()
    }

    void 'test h2 connection'() {
        when:
        List obfs = ObservationFact.all

        then:
        obfs.size() == 4
    }

    void 'test add solr info folderstudymapping'() {
        when:
        FolderStudyMappingView f = new FolderStudyMappingView([root: false, conceptPath: "\\\\Nikskkk\\hoi\\", folderId: 100, id: "Test123"])
        f.save()
        facetsIndexingService.indexByIds([
                new FacetsDocId('CONCEPT:\\Niks\\hoi\\'),] as Set)
        facetsIndexingService.flush()

        then:
        FolderStudyMappingView.all.size() == 1
    }

    void 'test add browse studies'() {
        when:
        BrowseStudiesView b = new BrowseStudiesView(id: "test", title: "test", description: "this is a test",
                design: "niemand", biomarkerType: "voet", accessType: "iedereen", accession: "noppe",
                institution: "the hyve", country: "The Netherlands", disease: "kroketten", compound: "vlees",
                studyObjective: "niets", organism: "Human", studyPhase: "einde")
        b.save()
        facetsIndexingService.fullIndex()
        facetsIndexingService.flush()

        then:
        BrowseStudiesView.all.size() == 1

    }

    void addDocument() {
        when:
        String id = 'FOO:12345'

        facetsIndexingService.addDocument(new FacetsDocument(
                facetsDocId: new FacetsDocId(id),
                fieldValues: ImmutableMultimap.of(),
        ))
        facetsIndexingService.flush()

        then:
        countDocuments("id:\"$id\"") == 1
    }

    void testRemoveDocument() {
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
        countDocuments('id:"FOO:12345"') == 0
    }

    void testGetAllDisplaySettings() {
        when:
        def allSettings = facetsQueryingService.allDisplaySettings
        def firstEntry = allSettings.entrySet()[0]

        then:
        //allSettings.size() == 2
        firstEntry.key == 'TEXT'
        firstEntry.value.displayName == 'Full Text'
        !firstEntry.value.hideFromListings
    }

    private int countDocuments(obj) {
        SolrQuery q = new SolrQuery(obj)
        q.set('rows', 0)
        solrFacetsCore.query(q).results.numFound
    }

}
