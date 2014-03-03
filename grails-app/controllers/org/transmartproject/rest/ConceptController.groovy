package org.transmartproject.rest

import grails.converters.JSON
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.core.ontology.Study
import org.transmartproject.db.concept.ConceptKey
import org.transmartproject.db.ontology.ConceptsResourceService

class ConceptController {

    static responseFormats = ['json', 'hal']

    StudyLoadingService studyLoadingServiceProxy
    ConceptsResourceService conceptsResourceService

    /** GET request on /studies/XXX/concepts/
     *  This will return the list of concepts, where each concept will be rendered in its short format
    */
    def index() {
        respond studyLoadingServiceProxy.study.ontologyTerm.allDescendants
    }

    /** GET request on /studies/XXX/concepts/${id}
     *  This returns the single requested entity.
     *
     *  @param id The id for which to return study information.
     */
    def show(String id) {
        String studyKey = studyLoadingServiceProxy.study.ontologyTerm.key
        String key = studyKey + getConceptPath(id)

        respond conceptsResourceService.getByKey(key)
    }


    private static String getConceptPath(String id) {
        id.replace("/", "\\")
    }

}
