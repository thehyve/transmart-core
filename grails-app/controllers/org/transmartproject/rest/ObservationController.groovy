package org.transmartproject.rest

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestContextHolder
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.clinical.ClinicalDataResource
import org.transmartproject.core.dataquery.clinical.PatientRow
import org.transmartproject.core.dataquery.clinical.PatientsResource
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.Study
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable
import org.transmartproject.db.ontology.ConceptsResourceService
import org.transmartproject.rest.marshallers.ObservationWrapper
import org.transmartproject.rest.ontology.OntologyTermCategory

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

    private Study getStudy() { studyLoadingServiceProxy.study }

    private OntologyTerm getConcept() {
        String conceptId = params.conceptId
        if (conceptId == null) {
            // should not happen
            throw new InvalidArgumentsException('Could not find a concept id')
        }

        use (OntologyTermCategory) {
            conceptsResourceService.getByKey(
                    conceptId.keyFromURLPart(study))
        }
    }

    private Patient getPatient() {
        Long subjectId = Long.parseLong(params.get('subjectId'))
        if (!subjectId) {
            // should not happen
            throw new InvalidArgumentsException('Could not find a study id')
        }

        def patient = patientsResourceService.getPatientById(subjectId)
        if (patient.trial != study.name) {
            throw new NoSuchResourceException("Subject $subjectId does not " +
                    "belong to study ${study.name}")
        }
        patient
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
