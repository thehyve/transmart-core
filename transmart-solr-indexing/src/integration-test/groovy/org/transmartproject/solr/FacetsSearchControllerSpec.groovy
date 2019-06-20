package org.transmartproject.solr

import grails.plugins.rest.client.RestBuilder
import grails.plugins.rest.client.RestResponse
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.grails.web.json.JSONArray
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import spock.lang.Ignore
import spock.lang.Specification

import static org.hamcrest.CoreMatchers.instanceOf
import static spock.util.matcher.HamcrestSupport.that

@Integration
@Rollback
class FacetsSearchControllerSpec extends Specification {

    static final String AUTOCOMPLETE_PATH = '/facetsSearch/autocomplete'
    static final String FACETS_RESULTS_PATH = '/facetsSearch/getFacetResults'

    @Autowired
    FacetsSearchController facetsSearchController

    @Autowired
    FacetsIndexingService facetsIndexingService

    @Value('${local.server.port}')
    Integer serverPort

    String getBaseURL() { "http://localhost:${serverPort}" }

    RestBuilder rest = new RestBuilder()

    RestResponse get(String path, Closure paramSetup = {}) {
        rest.get("${baseURL}${path}", paramSetup)
    }

    RestResponse post(String path, Closure paramSetup = {}) {
        rest.post("${baseURL}${path}", paramSetup)
    }

    void indexed() {
        facetsIndexingService.clearIndex()
        facetsIndexingService.fullIndex()
    }

    void 'testAutocompleteOneField' () {
        given:
        indexed()

        when:
        def response = get "${AUTOCOMPLETE_PATH}?category=test_tag_s&term=tes&requiredField=*"

        then:
        response.status == 200
        that response.json, instanceOf(JSONArray)
        response.json.size() == 1
        response.json[0].category == 'test_tag_s'
        response.json[0].value    == 'Test option 1'
        response.json[0].count    == 1
    }

    void 'testAutoCompleteAll' () {
        given:
        indexed()

        when:
        def response = get "${AUTOCOMPLETE_PATH}?category=*&term=tes&requiredField=*"

        then:
        response.status == 200
        that response.json, instanceOf(JSONArray)
        response.json.size() == 1
        response.json[0].category == 'test_tag_s'
        response.json[0].value    == 'Test option 1'
        response.json[0].count    == 1
    }

    void 'testSearchAllFields' () {
        given:
        indexed()

        when:
        def query = '''{
  "requiredField": "CONCEPT_PATH",
  "operator": "AND",
  "fieldTerms": {
    "*": {
      "operator": "OR",
      "searchTerms": [
        {"luceneTerm":"age"}
      ]
    }
  }
}'''
        def response = post FACETS_RESULTS_PATH, { ->
            contentType 'application/json'
            json query
        }

        then:
        response.status == 200
        response.json.numFound == 15
        that response.json.conceptKeys, instanceOf(JSONArray)
        response.json.conceptKeys.size() == 15
    }

}
