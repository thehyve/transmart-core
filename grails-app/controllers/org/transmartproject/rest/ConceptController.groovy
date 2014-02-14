package org.transmartproject.rest

import grails.converters.JSON
import org.transmartproject.core.ontology.ConceptsResource

class ConceptController {

    ConceptsResource conceptsResourceService

    /** GET request on /studies/XXX/concepts/
     *  This will return the list of concepts, where each concept will be rendered in its short format
     *
     * @param max The maximum amount of items of the list to be returned.
    */
    def index(Integer max) {
        //TODO
        render 'todo' as JSON
    }

    /** GET request on /studies/XXX/concepts/${id}
     *  This returns the single requested entity.
     *
     *  @param id The id for which to return study information.
     */
    def show(Integer id) {
        render 'todo' as JSON
    }

}
