package org.transmartproject.rest

import grails.converters.JSON
import org.transmartproject.core.ontology.ConceptsResource

class ConceptController {

    StudyLoadingService studyLoadingServiceProxy

    /** GET request on /studies/XXX/concepts/
     *  This will return the list of concepts, where each concept will be rendered in its short format
    */
    def index() {
        //TODO
        render 'todo' as JSON
    }

    /** GET request on /studies/XXX/concepts/${id}
     *  This returns the single requested entity.
     *
     *  @param id The id for which to return study information.
     */
    def show(Integer id) {
        //TODO
        render 'todo' as JSON
    }

}
