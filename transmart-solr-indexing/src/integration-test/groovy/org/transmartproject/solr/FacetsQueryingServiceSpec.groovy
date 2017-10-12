package org.transmartproject.solr

import grails.test.mixin.integration.Integration
import grails.transaction.Transactional
import org.hamcrest.CoreMatchers
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import spock.util.matcher.HamcrestMatchers

import static org.hamcrest.Matchers.hasKey
import static org.hamcrest.Matchers.hasSize
import static spock.util.matcher.HamcrestSupport.that

@Integration
@Transactional
class FacetsQueryingServiceSpec extends Specification {

    @Autowired
    FacetsQueryingService facetsQueryingService

    @Autowired
    FacetsIndexingService facetsIndexingService

    void 'testGetAllDisplaySettings' () {
        when:
        def allSettings = facetsQueryingService.allDisplaySettings
        def firstEntry = allSettings.entrySet()[0]

        then:
        assert allSettings.size() == 4
        assert firstEntry.key == 'TEXT'
        assert firstEntry.value.displayName == 'Full Text'
        assert !firstEntry.value.hideFromListings
    }

    void 'testGetTopTerms' () {
        when:
        def topTerms = facetsQueryingService.getTopTerms('*')

        then:
        //order by count, then by name
        that topTerms, hasKey('data_type_s')
        that topTerms, hasKey('test_tag_s')
        topTerms['data_type_s'][0] == new TermCount(term: 'Messenger RNA data (Microarray)', count: 8)
        topTerms['data_type_s'][1] == new TermCount(term: 'Transcript Level Messenger RNA data (Sequencing)', count: 2)
        topTerms['test_tag_s'][0] == new TermCount(term: 'Test option 1', count: 1)
    }
}
