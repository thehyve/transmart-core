package org.transmartproject.search.indexing

import grails.test.mixin.TestMixin
import grails.util.Holders
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

@TestMixin(RuleBasedIntegrationTestMixin)
class FacetsQueryingServiceTests {

    @Autowired
    FacetsQueryingService facetsQueryingService

    @Rule
    public TestRule skipIfSolrNotAvailableRule = new SkipIfSolrNotAvailableRule()

    @BeforeClass
    static void beforeClass() {
        Holders.applicationContext.getBean(FacetsIndexingService).clearIndex()
        Holders.applicationContext.getBean(FacetsIndexingService).fullIndex()
    }

    @Test
    void testGetAllDisplaySettings() {
        def allSettings = facetsQueryingService.allDisplaySettings

        assert allSettings.size() == 34

        def firstEntry = allSettings.entrySet()[0]

        assert firstEntry.key == 'TEXT'
        assert firstEntry.value.displayName == 'Full Text'
        assert !firstEntry.value.hideFromListings
    }

    @Test
    void testGetTopTerms() {
        def topTerms = facetsQueryingService.topTerms

        //order by count, then by name
        assert topTerms['access_type_s'][0] == new TermCount(term: 'Commercial', count: 2)
        assert topTerms['access_type_s'][1] == new TermCount(term: 'Consortium', count: 2)
    }
}
