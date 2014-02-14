package org.transmartproject.rest

import grails.converters.JSON
import org.transmartproject.core.dataquery.clinical.PatientsResource
import org.transmartproject.core.exceptions.NoSuchResourceException

class SubjectController {

    StudyLoadingService studyLoadingServiceProxy
    PatientsResource    patientsResourceService

    /** GET request on /studies/XXX/subjects/
     *  This will return the list of subjects, where each subject will be rendered in its short format
     *
     * @param max The maximum amount of items of the list to be returned.
    */
    def index() {
        render studyLoadingServiceProxy.study.patients as JSON
    }

    /** GET request on /studies/XXX/subjects/${id}
     *  This returns the single requested entity.
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

}

