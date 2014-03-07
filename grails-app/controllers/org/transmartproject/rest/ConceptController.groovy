package org.transmartproject.rest

import grails.converters.JSON
import grails.rest.Link
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.Study
import org.transmartproject.db.concept.ConceptKey
import org.transmartproject.db.ontology.ConceptsResourceService
import org.transmartproject.rest.marshallers.CollectionResponseWrapper
import org.transmartproject.rest.marshallers.OntologyTermSerializationHelper

class ConceptController {

    static responseFormats = ['json', 'hal']

    StudyLoadingService studyLoadingServiceProxy
    ConceptsResourceService conceptsResourceService

    /** GET request on /studies/XXX/concepts/
     *  This will return the list of concepts, where each concept will be rendered in its short format
    */
    def index() {
        respond wrapConcepts(studyLoadingServiceProxy.study.ontologyTerm.allDescendants)
    }

    /** GET request on /studies/XXX/concepts/${id}
     *  This returns the single requested entity.
     *
     *  @param id The id for which to return study information.
     */
    def show(String id) {
        String key = studyLoadingServiceProxy.study.ontologyTerm.key + OntologyTermSerializationHelper.idToPath(id)
        respond conceptsResourceService.getByKey(key)
    }

    /**
     * @param source
     * @return CollectionResponseWrapper so we can provide a proper HAL response
     */
    def wrapConcepts(Object source) {
        new CollectionResponseWrapper(
                collection: source,
                componentType: OntologyTerm,
                links: [
                        new Link(grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF,
                                "/studies/${studyLoadingServiceProxy.studyLowercase}/concepts"
                        )
                ]
        )
    }

}
