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
