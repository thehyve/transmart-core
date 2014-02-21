package org.transmartproject.rest

import org.transmartproject.core.ontology.StudiesResource

import javax.annotation.Resource

class StudyController {

    static responseFormats = ['json', 'hal']

    @Resource
    StudiesResource studiesResourceService

    /** GET request on /studies/
     *  This will return the list of studies, where each study will be rendered in its short format
    */
    def index() {
        respond studiesResourceService.studySet
    }

    /** GET request on /studies/${id}
     *  This returns the single study by name.
     *
     *  @param name the name of the study
     */
    def show(String id) {
        respond studiesResourceService.getStudyByName(id)
    }
}
