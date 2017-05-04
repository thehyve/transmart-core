package transmart.solr.indexing

import grails.test.mixin.TestMixin
import grails.test.mixin.integration.Integration
import grails.transaction.Transactional
import grails.util.Holders
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

@Integration
@Transactional
@Ignore
class FacetsQueryingServiceSpec extends Specification{

    @Autowired
    FacetsQueryingService facetsQueryingService

    //@Rule
    //public TestRule skipIfSolrNotAvailableRule = new SkipIfSolrNotAvailableRule()

    @Autowired
    FacetsIndexingService facetsIndexingService

    @Test
    void 'testGetAllDisplaySettings' () {
        when:
        def allSettings = facetsQueryingService.allDisplaySettings
        def firstEntry = allSettings.entrySet()[0]

        then:
        assert allSettings.size() == 34
        assert firstEntry.key == 'TEXT'
        assert firstEntry.value.displayName == 'Full Text'
        assert !firstEntry.value.hideFromListings
    }

    @Test
    void 'testGetTopTerms' () {
        when:
        def topTerms = facetsQueryingService.topTerms

        then:
        //order by count, then by name
        assert topTerms['access_type_s'][0] == new TermCount(term: 'Commercial', count: 2)
        assert topTerms['access_type_s'][1] == new TermCount(term: 'Consortium', count: 2)
    }
}
