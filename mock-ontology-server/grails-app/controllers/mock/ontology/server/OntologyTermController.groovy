package mock.ontology.server

import grails.converters.*

class OntologyTermController {
    static responseFormats = ['json', 'xml']

    /**
     * Reference terminology search endpoint:
     * <code>/search/idx</code>
     *
     * @param conceptCode
     * @return
     */
    def index() {
        def concepts = new OntologyTermResponseGenerator().fetchPreferredConcepts(params.conceptCode)
        render concepts as JSON
    }

    /**
     * Terminology identifier endpoint:
     * <code>/idx</code>
     *
     * @param idx
     * @return
     */
    def show() {
        //TODO: sample response, do not have details of what it should return
        def details = new OntologyTermResponseGenerator().getDetails(params.roxId)
        render details as JSON
    }

}

