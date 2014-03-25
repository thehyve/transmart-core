package org.transmartproject.rest

import grails.rest.Link
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.rest.marshallers.CollectionResponseWrapper
import org.transmartproject.rest.marshallers.OntologyTermWrapper
import org.transmartproject.rest.ontology.OntologyTermCategory

class ConceptController {

    static responseFormats = ['json', 'hal']

    StudyLoadingService studyLoadingServiceProxy
    ConceptsResource conceptsResourceService

    /** GET request on /studies/XXX/concepts/
     *  This will return the list of concepts, where each concept will be rendered in its short format
    */
    def index() {
        def concepts = studyLoadingServiceProxy.study.ontologyTerm.allDescendants
        def conceptWrappers = OntologyTermWrapper.wrap(concepts)
        respond wrapConcepts(conceptWrappers)
    }

    /** GET request on /studies/XXX/concepts/${id}
     *  This returns the single requested entity.
     *
     *  @param id The id for which to return study information.
     */
    def show(String id) {
        use (OntologyTermCategory) {
            String key = id.keyFromURLPart studyLoadingServiceProxy.study
            def concept = conceptsResourceService.getByKey(key)
            respond new OntologyTermWrapper(concept)
        }
    }

    /**
     * @param source
     * @return CollectionResponseWrapper so we can provide a proper HAL response
     */
    def wrapConcepts(Object source) {
        new CollectionResponseWrapper(
                collection: source,
                componentType: OntologyTermWrapper,
                links: [
                        new Link(grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF,
                                "/studies/${studyLoadingServiceProxy.studyLowercase}/concepts"
                        )
                ]
        )
    }

}
