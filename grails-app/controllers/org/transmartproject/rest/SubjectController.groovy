package org.transmartproject.rest

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.transmartproject.db.ontology.ConceptsResourceService
import org.transmartproject.db.ontology.I2b2

// @Secured(['ROLE_USER'])
@Secured(['permitAll'])
class SubjectController {

    ConceptsResourceService conceptsResourceService

    /** GET request on /studies/XXX/subjects/
     *  This will return the list of studies, where each study will be rendered in its short format
     *
     * @param max The maximum amount of items of the list to be returned.
    */
    def index(Integer max) {
    	log.info "params:$params"
        I2b2 study = conceptsResourceService.getStudy(params.studyId)
        log.info "study: ${study.name}"
        if (!study) {
            def error = ['error':'study not found']
            render error as JSON
            return
        }
        def results = conceptsResourceService.getSubjectsForStudy(study)
        render results as JSON
    }

    /** GET request on /studies/${id}
     *  This returns the single requested entity.
     *
     *  @param id The is for which to return study information.
     */
    def show(Integer id) {
        log.info "in show method for study with id:$id"
        render "todo" as JSON
    }

}