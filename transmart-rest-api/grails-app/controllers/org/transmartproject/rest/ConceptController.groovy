package org.transmartproject.rest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.transmartproject.core.concept.ConceptsResource

class ConceptController extends AbstractQueryController {

    static responseFormats = ['json']

    @Autowired
    ConceptsResource conceptsResource

    /**
     * Fetches all concepts.
     *
     * @return the list of concepts as JSON.
     */
    def index() {
        checkParams(params, [])
        respond concepts: conceptsResource.getConcepts(currentUser)
    }

    /**
     * Fetches the concept with the conceptCode if it exists.
     *
     * @param conceptCode the conceptCode of the concept.
     * @return the concept as JSON.
     * @throws org.transmartproject.core.exceptions.NoSuchResourceException
     * if the concept does not exists or the user does not have access to it.
     */
    def show(@PathVariable('conceptCode') String conceptCode) {
        checkParams(params, ['conceptCode'])
        respond conceptsResource.getConceptByConceptCodeForUser(conceptCode, currentUser)
    }

}
