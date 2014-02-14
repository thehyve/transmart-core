package org.transmartproject.rest

import grails.converters.JSON
import org.transmartproject.core.dataquery.clinical.ClinicalDataResource

class ObservationController {

    ClinicalDataResource clinicalDataResourceService

    /** GET request on /studies/XXX/observations/
     *  This will return the list of observations for study XXX
     */
    def index() {
        log.info "params:$params"
        def results = clinicalDataResourceService.retrieveData([], []/*,
                [ new TerminalConceptVariable(conceptCode: 'mirnastudy') ]*/)
        render results as JSON
    }

    /** GET request on /studies/XXX/concepts/YYY/observations/
     *  This will return the list of observations for study XXX and concept YYY
     */
    def indexByConcept() {
        //TODO
        render "todo" as JSON
    }

    /** GET request on /studies/XXX/subjects/YYY/observations/
     *  This will return the list of observations for study XXX and subject YYY
     */
    def indexBySubject() {
        //TODO
        render "todo" as JSON
    }

}
