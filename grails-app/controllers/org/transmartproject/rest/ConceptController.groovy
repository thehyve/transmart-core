package org.transmartproject.rest

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.transmartproject.db.ontology.ConceptsResourceService
import org.transmartproject.db.ontology.I2b2

@Secured(['IS_AUTHENTICATED_REMEMBERED'])
class ConceptController {

    ConceptsResourceService conceptsResourceService

    /** GET request on /studies/XXX/concepts/
     *  This will return the list of concepts, where each concept will be rendered in its short format
     *
     * @param max The maximum amount of items of the list to be returned.
    */
    def index(Integer max) {
        // TODO The service call actually uses the name that is not guaranteed unique. This sucks, improve.
        // TODO error handling
        I2b2 study = conceptsResourceService.getStudy(params.studyId)
        if (!study) {
            def error = ['error':'study not found']
            render error as JSON
            return
        }
        def results = conceptsResourceService.getConceptsForStudy(study)
        render results as JSON
    }

    /** GET request on /studies/XXX/concepts/${id}
     *  This returns the single requested entity.
     *
     *  @param id The id for which to return study information.
     */
    def show(Integer id) {
        def subject = conceptsResourceService.getConceptForStudy(id)
        render subject as JSON
    }

}