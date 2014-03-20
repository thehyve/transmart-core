package org.transmartproject.rest

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestContextHolder
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.clinical.ClinicalDataResource
import org.transmartproject.core.dataquery.clinical.PatientRow
import org.transmartproject.core.dataquery.clinical.PatientsResource
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.Study
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable
import org.transmartproject.db.ontology.ConceptsResourceService
import org.transmartproject.rest.marshallers.ObservationWrapper

class ObservationController {

    static responseFormats = ['json', 'hal']

    ClinicalDataResource clinicalDataResourceService
    StudyLoadingService studyLoadingServiceProxy
    PatientsResource patientsResourceService
    ConceptsResourceService conceptsResourceService

    /** GET request on /studies/XXX/observations/
     *  This will return the list of observations for study XXX
     */
    def index() {
        def study = studyLoadingServiceProxy.study
        TabularResult<TerminalConceptVariable, PatientRow> observations =
                clinicalDataResourceService.retrieveData(study.patients, [study.ontologyTerm] as Set)
        try {
            respond wrapObservations(observations)
        } finally {
            observations.close()
        }
    }

    /** GET request on /studies/XXX/concepts/YYY/observations/
     *  This will return the list of observations for study XXX and concept YYY
     */
    def indexByConcept() {
        TabularResult<TerminalConceptVariable, PatientRow> observations =
                clinicalDataResourceService.retrieveData(study.patients, [concept] as Set)
        try {
            respond wrapObservations(observations)
        } finally {
            observations.close()
        }
    }

    /** GET request on /studies/XXX/subjects/YYY/observations/
     *  This will return the list of observations for study XXX and subject YYY
     */
    def indexBySubject() {
        TabularResult<TerminalConceptVariable, PatientRow> observations =
                clinicalDataResourceService.retrieveData([patient] as Set, [study.ontologyTerm] as Set)
        try {
            respond wrapObservations(observations)
        } finally {
            observations.close()
        }
    }

    Study getStudy() { studyLoadingServiceProxy.study }

    OntologyTerm getConcept() {
        GrailsWebRequest webRequest = RequestContextHolder.currentRequestAttributes()
        String conceptId = webRequest.params.get('conceptId')
        if (!conceptId) {
            throw new InvalidArgumentsException('Could not find a concept id')
        }
        String studyKey = studyLoadingServiceProxy.study.ontologyTerm.key

        conceptsResourceService.getByKey(studyKey + getConceptPath(conceptId))
    }

    //TODO Move this method to some other place
    private static String getConceptPath(String id) {
        id.replace("/", "\\")
    }

    Patient getPatient() {
        GrailsWebRequest webRequest = RequestContextHolder.currentRequestAttributes()
        Long subjectId = Long.parseLong(webRequest.params.get('subjectId'))
        if (!subjectId) {
            throw new InvalidArgumentsException('Could not find a study id')
        }
        patientsResourceService.getPatientById(subjectId)
    }

    private static List<ObservationWrapper> wrapObservations(TabularResult<TerminalConceptVariable, PatientRow> tabularResult) {
        List<ObservationWrapper> observations = []
        def concepts = tabularResult.getIndicesList()
        tabularResult.getRows().each { row ->
            concepts.each {concept ->
                def value = row.getAt(concept)
                observations << new ObservationWrapper(
                        subject: row.patient,
                        concept: concept,
                        value: value
                )
            }
        }
        observations
    }
}
