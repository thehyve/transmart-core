package org.transmartproject.search

import grails.test.mixin.TestMixin
import grails.util.Holders
import org.junit.Before
import org.junit.BeforeClass
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin
import org.transmartproject.search.indexing.FacetsIndexingService

@TestMixin(RuleBasedIntegrationTestMixin)
class FacetsSearchControllerTests {

    FacetsSearchController facetsSearchController

    @BeforeClass
    static void beforeClass() {
        Holders.applicationContext.getBean(FacetsIndexingService).clearIndex()
        Holders.applicationContext.getBean(FacetsIndexingService).fullIndex()
    }

    @Before
    void before() {
        facetsSearchController = new FacetsSearchController()
    }

    void testAutocompleteOneField() {
        def command = new AutoCompleteCommand(category: 'therapeutic_domain_s', term: 'b')
        facetsSearchController.autocomplete(command)

        def resp = facetsSearchController.response.json
        assert resp[0].category == 'therapeutic_domain_s'
        assert resp[0].value    == 'Behaviors and Mental Disorders'
        assert resp[0].count    == 1
    }

    void testAutoCompleteAll() {
        def command = new AutoCompleteCommand(category: '*', term: 'e', requiredField: 'CONCEPT_PATH')
        facetsSearchController.autocomplete(command)

        def resp = facetsSearchController.response.json

        assert resp.size() == 3
        assert resp[0].category == 'biomarker_type_s'
        assert resp[0].value    == 'Efficacy biomarker'
        assert resp[0].count    == 4
    }

    void testSearchAllFields() {
        def command = new GetFacetsCommand(operator: 'OR',
                fieldTerms: ['*': new FieldTerms(operator: 'OR',
                        searchTerms: [
                                new SearchTerm(literalTerm: 'age'),
                                new SearchTerm(literalTerm: 'FOLDER:1992455'),
                        ])])

        facetsSearchController.getFacetResults(command)

        assert facetsSearchController.response.json.numFound == 4
    }

    void testSearchAllFieldsNumber() {
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

        assert facetsSearchController.response.json.numFound == 2
    }
}
