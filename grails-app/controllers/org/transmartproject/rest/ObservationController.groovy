package org.transmartproject.rest

import grails.converters.JSON
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestContextHolder
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.clinical.ClinicalDataResource
import org.transmartproject.core.dataquery.clinical.PatientRow
import org.transmartproject.core.dataquery.clinical.PatientsResource
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable
import org.transmartproject.db.i2b2data.ObservationFact

class ObservationController {

    static responseFormats = ['json', 'hal']

    ClinicalDataResource clinicalDataResourceService
    StudyLoadingService studyLoadingServiceProxy
    PatientsResource    patientsResourceService

    /** GET request on /studies/XXX/observations/
     *  This will return the list of observations for study XXX
     */
    def index() {
        def study = studyLoadingServiceProxy.study

        TabularResult<TerminalConceptVariable, PatientRow> observations =
            clinicalDataResourceService.retrieveData() //TODO
        respond observations
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
        // Retrieve study
        def study = studyLoadingServiceProxy.study

        // Retrieve subject
        GrailsWebRequest webRequest = RequestContextHolder.currentRequestAttributes()
        Long subjectId = Long.parseLong(webRequest.params.get('subjectId'))
        if (!subjectId) {
            throw new InvalidArgumentsException('Could not find a study id')
        }
        def subject = patientsResourceService.getPatientById(subjectId)

        //TODO: check whether subject actually belongs to the study

        // Retrieve observations
        def observations = ObservationFact.withCriteria {
            like 'sourcesystemCd', "${study.name}%"
            eq 'patient', subject
        }
        respond observations
    }

}
