package org.transmartproject.rest

import grails.converters.JSON
import org.transmartproject.webservices.Observation
import grails.plugin.springsecurity.annotation.Secured
import org.transmartproject.core.ontology.ConceptsResource

// @Secured(['ROLE_ADMIN'])
@Secured(['permitAll'])
class StudyController {

    ConceptsResource conceptsResourceService

    /** GET request on /studies/
     *  This will return the list of studies, where each study will be rendered in its short format
     *
     * @param max The maximum amount of items of the list to be returned.
    */
    def index(Integer max) {
        // TODO Implement max?
        render conceptsResourceService.getAllStudies() as JSON
    }

    /** GET request on /studies/${id}
     *  This returns the single requested entity.
     *
     *  @param id The is for which to return study information.
     */
    def show(Integer id) {
        render conceptsResourceService.getStudy(params.id) as JSON
    }

    /*
     *    GET /studies            index
     *    GET /studies/create     create
     *    POST    /studies        save
     *    GET /studies/${id}      show
     *    GET /studies/${id}/edit edit
     *    PUT /studies/${id}      update
     *    DELETE  /studies/${id}  delete
     */
}
