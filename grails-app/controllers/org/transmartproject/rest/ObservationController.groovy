package org.transmartproject.rest

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.clinical.*
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.Study
import org.transmartproject.rest.marshallers.ObservationWrapper
import org.transmartproject.rest.ontology.OntologyTermCategory

class ObservationController {

    static responseFormats = ['json', 'hal']

    ClinicalDataResource clinicalDataResourceService
    StudyLoadingService studyLoadingServiceProxy
    PatientsResource patientsResourceService
    ConceptsResource conceptsResourceService

    /** GET request on /studies/XXX/observations/
     *  This will return the list of observations for study XXX
     */
    def index() {
        def study = studyLoadingServiceProxy.study
        TabularResult<ClinicalVariable, PatientRow> observations =
                clinicalDataResourceService.retrieveData(study.patients,
                        [createClinicalVariable(study.ontologyTerm)])
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
        TabularResult<ClinicalVariable, PatientRow> observations =
                clinicalDataResourceService.retrieveData(study.patients,
                        [createClinicalVariable(concept)])
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
        TabularResult<ClinicalVariable, PatientRow> observations =
                clinicalDataResourceService.retrieveData([patient] as Set,
                        [createClinicalVariable(study.ontologyTerm)])
        try {
            respond wrapObservations(observations)
        } finally {
            observations.close()
        }
    }

    private ClinicalVariable createClinicalVariable(OntologyTerm term) {
        clinicalDataResourceService.createClinicalVariable(
                ClinicalVariable.NORMALIZED_LEAFS_VARIABLE,
                concept_code: term.code)
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

    private static List<ObservationWrapper> wrapObservations(
            TabularResult<ClinicalVariable, PatientRow> tabularResult) {
        List<ObservationWrapper> observations = []
        def variableColumns = tabularResult.getIndicesList()
        tabularResult.getRows().each { row ->
            variableColumns.each { ClinicalVariableColumn topVar ->
                def value = row.getAt(topVar)

                if (value instanceof Map) {
                    value.each { ClinicalVariableColumn var, Object obj ->
                        observations << new ObservationWrapper(
                                subject: row.patient,
                                label: var.label,
                                value: obj)
                    }
                } else {
                    observations << new ObservationWrapper(
                            subject: row.patient,
                            label: topVar.label,
                            value: value)
                    }
            }
        }

        observations
    }
}
