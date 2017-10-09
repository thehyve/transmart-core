package org.transmartproject.solr

import grails.test.mixin.integration.Integration
import grails.transaction.Transactional
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

@Integration
@Transactional
@Ignore
class FacetsSearchControllerTests {

    FacetsSearchController facetsSearchController

    @Autowired
    FacetsIndexingService facetsIndexingService

    @Before
    void before() {
        facetsIndexingService.clearIndex()
        facetsIndexingService.fullIndex()
        facetsSearchController = new FacetsSearchController()
    }

    @Test
    void 'testAutocompleteOneField' () {
        when:
        def command = new AutoCompleteCommand(category: 'therapeutic_domain_s', term: 'b')
        facetsSearchController.autocomplete(command)

        def resp = facetsSearchController.response.json

        then:
        assert resp[0].category == 'therapeutic_domain_s'
        assert resp[0].value    == 'Behaviors and Mental Disorders'
        assert resp[0].count    == 1
    }

    void 'testAutoCompleteAll' () {
        when:
        def command = new AutoCompleteCommand(category: '*', term: 'e', requiredField: 'CONCEPT_PATH')
        facetsSearchController.autocomplete(command)

        def resp = facetsSearchController.response.json

        then:
        assert resp.size() == 3
        assert resp[0].category == 'biomarker_type_s'
        assert resp[0].value    == 'Efficacy biomarker'
        assert resp[0].count    == 4
    }

    void 'testSearchAllFields' () {
        when:
        def command = new GetFacetsCommand(operator: 'OR',
                fieldTerms: ['*': new FieldTerms(operator: 'OR',
                        searchTerms: [
                                new SearchTerm(literalTerm: 'age'),
                                new SearchTerm(literalTerm: 'FOLDER:1992455'),
                        ])])

        facetsSearchController.getFacetResults(command)

        then:
        assert facetsSearchController.response.json.numFound == 4
    }

    void 'testSearchAllFieldsNumber' () {
        when:
        def command = new GetFacetsCommand(operator: 'AND',
                fieldTerms: [
                        '*': new FieldTerms(operator: 'OR',
                                searchTerms: [
                                        new SearchTerm(literalTerm: '58'),
                                        new SearchTerm(luceneTerm: '[18 TO 20]'),
                                ]),
                        id: new FieldTerms(operator: 'OR',
                                searchTerms: [
                                        new SearchTerm(luceneTerm: 'FOLDER\\:*'),
                                ]),
                ])

        facetsSearchController.getFacetResults(command)

        then:
        assert facetsSearchController.response.json.numFound == 2
    }
}
