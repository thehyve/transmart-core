package org.transmartproject.rest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.transmartproject.core.concept.Concept
import org.transmartproject.core.concept.ConceptsResource
import org.transmartproject.rest.marshallers.ConceptModifiersWrapper
import org.transmartproject.rest.marshallers.ConceptWrapper
import org.transmartproject.rest.marshallers.ContainerResponseWrapper

import static org.transmartproject.rest.misc.RequestUtils.checkForUnsupportedParams

class ConceptController extends AbstractQueryController {

    static responseFormats = ['json', 'hal']

    @Autowired
    ConceptsResource conceptsResource

    /**
     * Fetches all concepts.
     *
     * @return the list of concepts as JSON.
     */
    def index(@RequestParam('api_version') String apiVersion) {
        checkForUnsupportedParams(params, [])
        respond wrapConcepts(apiVersion, conceptsResource.getConcepts(authContext.user))
    }

    /**
     * Fetches the concept with the conceptCode if it exists.
     *
     * @param conceptCode the conceptCode of the concept.
     * @return the concept as JSON.
     * @throws org.transmartproject.core.exceptions.NoSuchResourceException
     * if the concept does not exists or the user does not have access to it.
     */
    def show(@RequestParam('api_version') String apiVersion, @PathVariable('conceptCode') String conceptCode) {
        checkForUnsupportedParams(params, ['conceptCode'])
        respond new ConceptWrapper(
                apiVersion: apiVersion,
                concept: conceptsResource.getConceptByConceptCodeForUser(conceptCode, authContext.user)
        )
    }

    def getModifiers(@RequestParam('api_version') String apiVersion, @PathVariable('conceptCode') String conceptCode) {
        checkForUnsupportedParams(params, ['conceptCode'])
        respond new ConceptModifiersWrapper(
                modifiers: conceptsResource.getModifiersByConceptCode(conceptCode, authContext.user)
        )
    }

    private static wrapConcepts(String apiVersion, Collection<Concept> source) {
        new ContainerResponseWrapper(
                key: 'concepts',
                container: source.collect { new ConceptWrapper(apiVersion: apiVersion, concept: it) },
                componentType: Concept,
        )
    }

}
