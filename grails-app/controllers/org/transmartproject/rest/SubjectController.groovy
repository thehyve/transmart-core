package org.transmartproject.rest

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.transmartproject.db.ontology.ConceptsResourceService
import org.transmartproject.db.ontology.I2b2

// @Secured(['permitAll'])
@Secured(['ROLE_USER'])
class SubjectController {

    ConceptsResourceService conceptsResourceService

    /** GET request on /studies/XXX/subjects/
     *  This will return the list of subjects, where each subject will be rendered in its short format
     *
     * @param max The maximum amount of items of the list to be returned.
    */
    def index(Integer max) {
    	log.info "params:$params"
        // TODO The service call actually uses the name that is not guaranteed unique. This sucks, improve.
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

    /** GET request on /studies/XXX/subjects/${id}
     *  This returns the single requested entity.
     *
     *  @param id The is for which to return concept information.
     */
    def show(Integer id) {
        log.info "in show method for with id:$id"
        def subject = conceptsResourceService.getSubjectForStudy(id)
        render subject as JSON
    }

}