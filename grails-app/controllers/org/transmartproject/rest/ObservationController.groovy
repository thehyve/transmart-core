package org.transmartproject.rest

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.transmartproject.db.clinical.ClinicalDataResourceService
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable

// @Secured(['ROLE_USER'])
@Secured(['permitAll'])
class ObservationController {

    ClinicalDataResourceService clinicalDataResourceService

    /** GET request on /studies/XXX/observations/
     *  This will return the list of studies, where each study will be rendered in its short format
     *
     * @param max The maximum amount of items of the list to be returned.
    */
    def index(Integer max) {
    	log.info "params:$params"
        def results = clinicalDataResourceService.retrieveData([], []/*,
                [ new TerminalConceptVariable(conceptCode: 'mirnastudy') ]*/)
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