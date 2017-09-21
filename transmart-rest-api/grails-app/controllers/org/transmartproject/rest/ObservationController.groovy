/*
 * Copyright 2014 Janssen Research & Development, LLC.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version, along with the following terms:
 *
 *   1. You may convey a work based on this program in accordance with
 *      section 5, provided that you retain the above notices.
 *   2. You may convey verbatim copies of this program code as you receive
 *      it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.rest

import com.google.common.collect.AbstractIterator
import grails.web.RequestParameter
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.clinical.*
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.OntologyTermsResource
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.Study
import org.transmartproject.core.querytool.QueriesResource
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.rest.marshallers.ObservationWrapper
import org.transmartproject.rest.marshallers.PatientWrapper
import org.transmartproject.rest.misc.ComponentIndicatingContainer
import org.transmartproject.rest.ontology.OntologyTermCategory

import static org.transmartproject.core.dataquery.clinical.ClinicalVariable.NORMALIZED_LEAFS_VARIABLE

class ObservationController {

    static responseFormats = ['json', 'hal']

    ClinicalDataResource clinicalDataResourceService
    StudyLoadingService studyLoadingServiceProxy
    PatientsResource patientsResourceService
    OntologyTermsResource conceptsResourceService
    QueriesResource queriesResourceService

    /** GET request on /v1/studies/XXX/observations/
     *  This will return the list of observations for study XXX
     */
    def index() {
        def study = studyLoadingServiceProxy.study
        def variables = [createClinicalVariable(study.ontologyTerm)]
        TabularResult<ClinicalVariable, PatientRow> observations =
                clinicalDataResourceService.retrieveData(
                        study, variables)
        try {
            respond wrapObservations(observations, variables)
        } finally {
            observations.close()
        }
    }

    /**
     * GET /v1/observations
     * Not bound to a study.
     *
     * Parameters: patient_sets  => list of result instance ids or
     *                              single patient set id (integer)
     *                              Cannot be combined with patients
     *             patients      => list of patient ids or
     *                              single patient id (integer)
     *                              Cannot be combined with patient_sets.
     *                              Avoid specifying a large list for
     *                              performance reasons
     *             concept_paths => list of concepts paths or
     *                              single concept path (string).
     *                              Mandatory
     *
     *             variable_type => the type of clinical variable to create.
     *                              Defaults to 'normalized_leafs_variable'
     *
     * List
     */
    def indexStandalone(@RequestParameter('variable_type') String variableType) {
        List<Long> resultInstanceIds
        List<Long> patientIds
        List<String> conceptPaths
        wrapException(NumberFormatException) {
            resultInstanceIds = params.getList('patient_sets')
                    .collect { it as Long }
            patientIds = params.getList('patients').collect { it as Long }
            conceptPaths = params.getList('concept_paths')
        }

        if (resultInstanceIds && patientIds) {
            throw new InvalidArgumentsException('patient_sets and patients ' +
                    'cannot be given simultaneously')
        }
        if (!resultInstanceIds && !patientIds) {
            throw new InvalidArgumentsException(
                    'Either patient_sets or patients must be given')
        }
        if (!conceptPaths) {
            throw new InvalidArgumentsException(
                    'At least one concept path must be given')
        }

        List<ClinicalVariable> variables
        wrapException(IllegalArgumentException) {
            variables = conceptPaths.collect {
                clinicalDataResourceService.createClinicalVariable(
                        variableType ?: NORMALIZED_LEAFS_VARIABLE,
                        concept_path: it)
            }
        }

        TabularResult<ClinicalVariable, PatientRow> observations

        if (resultInstanceIds) {
            List<QueryResult> queryResults
            wrapException(NoSuchResourceException) {
                queryResults = resultInstanceIds.collect {
                    queriesResourceService.getQueryResultFromId(it)
                }
            }

            observations = clinicalDataResourceService.retrieveData(
                    queryResults, variables)
        } else {
            Set<Patient> patients
            wrapException(NoSuchResourceException) {
                patients = patientIds.collect {
                    patientsResourceService.getPatientById(it)
                }
            }

            observations = clinicalDataResourceService.retrieveData(
                    patients, variables)
        }

        try {
            respond wrapObservations(observations, variables)
        } finally {
            observations.close()
        }
    }

    /** GET request on /v1/studies/XXX/concepts/YYY/observations/
     *  This will return the list of observations for study XXX and concept YYY
     */
    def indexByConcept() {
        def variables = [createClinicalVariable(concept)]
        TabularResult<ClinicalVariable, PatientRow> observations =
                clinicalDataResourceService.retrieveData(
                        study, variables)
        try {
            respond wrapObservations(observations, variables)
        } finally {
            observations.close()
        }
    }

    /** GET request on /v1/studies/XXX/subjects/YYY/observations/
     *  This will return the list of observations for study XXX and subject YYY
     */
    def indexBySubject() {
        def variables = [createClinicalVariable(study.ontologyTerm)]
        TabularResult<ClinicalVariable, PatientRow> observations =
                clinicalDataResourceService.retrieveData(
                        [patient] as Set, variables)
        try {
            respond wrapObservations(observations, variables)
        } finally {
            observations.close()
        }
    }

    private ClinicalVariable createClinicalVariable(OntologyTerm term) {
        clinicalDataResourceService.createClinicalVariable(
                NORMALIZED_LEAFS_VARIABLE,
                concept_path: term.fullName)
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
        if (patient.trial != study.id) {
            throw new NoSuchResourceException("Subject $subjectId does not " +
                    "belong to study ${study.id}")
        }
        patient
    }

    private static Iterator<ObservationWrapper> wrapObservations(
            TabularResult<ClinicalVariable, PatientRow> tabularResult,
            List<ClinicalVariable> originalVariables) {
        new TabularResultObservationsIterator(
                originalRowIterator: tabularResult.getRows(),
                clinicalVariables: originalVariables,)
    }

    static class TabularResultObservationsIterator
            extends AbstractIterator<ObservationWrapper>
            implements ComponentIndicatingContainer {

        final Class<?> componentType = ObservationWrapper

        Iterator<PatientRow> originalRowIterator
        List<ClinicalVariable> clinicalVariables

        private PatientRow currentRow
        private Iterator<ClinicalVariableColumn> clinicalVariableIterator
        private Iterator<Map.Entry<ClinicalVariableColumn, Object>> complexCellIterator

        @Override
        protected ObservationWrapper computeNext() {
            if (complexCellIterator) {
                if (complexCellIterator.hasNext()) {
                    Map.Entry<ClinicalVariableColumn, Object> entry =
                            complexCellIterator.next()

                    return new ObservationWrapper(
                            subject: new PatientWrapper(apiVersion: 'v1', patient: currentRow.patient),
                            label: entry.key.label,
                            value: entry.value)
                } else {
                    complexCellIterator = null
                }
            }

            if (!clinicalVariableIterator?.hasNext()) {
                if (!originalRowIterator.hasNext()) {
                    return endOfData()
                }

                currentRow = originalRowIterator.next()
                clinicalVariableIterator = clinicalVariables.iterator()
            }

            def currentVar = clinicalVariableIterator.next()
            def value = currentRow.getAt(currentVar)

            if (value instanceof Map) {
                complexCellIterator = value.iterator()
                computeNext()
            } else {
                new ObservationWrapper(
                        subject: new PatientWrapper(apiVersion: 'v1', patient: currentRow.patient),
                        label: currentVar.label,
                        value: value)
            }
        }
    }

    private static void wrapException(Class<? extends Exception> exceptionType,
                                      Closure<Void> code) {
        try {
            code.call()
        } catch (Exception e) {
            if (exceptionType.isAssignableFrom(e.getClass())) {
                throw new InvalidArgumentsException(e)
            } else {
                throw e
            }
        }
    }
}
