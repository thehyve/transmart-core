package org.transmartproject.rest

import grails.converters.JSON
import org.transmartproject.core.dataquery.clinical.PatientsResource
import org.transmartproject.core.exceptions.NoSuchResourceException

class SubjectController {

    StudyLoadingService studyLoadingServiceProxy
    PatientsResource    patientsResourceService

    /** GET request on /studies/XXX/subjects/
     *  This will return the list of subjects for certain study,
     *  where each subject will be rendered in its short format
    */
    def index() {
        render studyLoadingServiceProxy.study.patients as JSON
    }

    /** GET request on /studies/XXX/subjects/${id}
     *  This returns the single subject for certain study.
     *
     *  @param id The is for which to return concept information.
     */
    def show(Integer id) {
        def patient = patientsResourceService.getPatientById(id)

        def studyName = studyLoadingServiceProxy.study.name
        if (patient.trial != studyName) {
            throw new NoSuchResourceException("The patient with id $id " +
                    "does not belong to the study '$studyName'")
        }

        render patient as JSON
    }

    /** GET request on /studies/XXX/concepts/YYY/subjects
     *
     * @return list of subjects for study XXX and concept YYY
     */
    def indexByConcept() {
        render "todo" as JSON
    }

}

