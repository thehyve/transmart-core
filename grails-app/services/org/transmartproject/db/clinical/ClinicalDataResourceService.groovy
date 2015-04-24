/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.clinical

import com.google.common.collect.Maps
import groovy.util.logging.Log4j
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.clinical.*
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.ontology.Study
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.dataquery.clinical.ClinicalDataTabularResult
import org.transmartproject.db.dataquery.clinical.InnerClinicalTabularResultFactory
import org.transmartproject.db.dataquery.clinical.PatientQuery
import org.transmartproject.db.dataquery.clinical.patientconstraints.PatientSetsConstraint
import org.transmartproject.db.dataquery.clinical.patientconstraints.StudyPatientsConstraint
import org.transmartproject.db.dataquery.clinical.variables.ClinicalVariableFactory
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable
import org.transmartproject.db.i2b2data.PatientDimension

@Log4j
class ClinicalDataResourceService implements ClinicalDataResource {

    static transactional = false

    def sessionFactory

    @Autowired
    ClinicalVariableFactory clinicalVariableFactory

    @Autowired
    InnerClinicalTabularResultFactory innerResultFactory

    @Override
    TabularResult<ClinicalVariableColumn, PatientRow> retrieveData(List<QueryResult> queryResults,
                                           List<ClinicalVariable> variables) {
        def patientQuery = new PatientQuery([
                new PatientSetsConstraint(queryResults)
        ])
        retrieveDataImpl(patientQuery, variables)
    }

    @Override
    TabularResult<ClinicalVariableColumn, PatientRow> retrieveData(Study study,
                                                                   List<ClinicalVariable> variables) {

        def studyPatientsQuery = new PatientQuery([
                new StudyPatientsConstraint(study)
        ])
        retrieveDataImpl(studyPatientsQuery, variables)
    }

    @Override
    TabularResult<ClinicalVariableColumn, PatientRow> retrieveData(Set<Patient> patientCollection,
                                                                   List<ClinicalVariable> variables) {
        retrieveDataImpl(patientCollection, variables)
    }

    private TabularResult<ClinicalVariableColumn, PatientRow> retrieveDataImpl(Iterable<PatientDimension> patients,
                                                                   List<ClinicalVariable> variables) {

        if (!variables) {
            throw new InvalidArgumentsException(
                    'No variables passed to #retrieveData()')
        }

        def session = sessionFactory.openStatelessSession()

        try {
            def patientMap = Maps.newTreeMap()

            patients.each { patientMap[it.id] = it }

            List<TerminalConceptVariable> flattenedVariables = []
            flattenClinicalVariables(flattenedVariables, variables)

            def intermediateResults = []
            if (patientMap) {
                intermediateResults = innerResultFactory.
                        createIntermediateResults(session,
                                patients, flattenedVariables)
            } else {
                log.info("No patients passed to retrieveData() with" +
                        "variables $variables; will skip main queries")
            }

            new ClinicalDataTabularResult(
                    session, intermediateResults, patientMap)
        } catch (Throwable t) {
            session.close()
            throw t
        }
    }

    private void flattenClinicalVariables(List<TerminalConceptVariable> target,
                                          List<ClinicalVariable> variables) {
        variables.each { var ->
            if (var instanceof ComposedVariable) {
                flattenClinicalVariables target, var.innerClinicalVariables
            } else {
                target << var
            }
        }
    }

    @Override
    TabularResult<ClinicalVariableColumn, PatientRow> retrieveData(QueryResult patientSet,
                                                                   List<ClinicalVariable> variables) {
        assert patientSet

        retrieveData([patientSet], variables)
    }

    @Override
    ClinicalVariable createClinicalVariable(Map<String, Object> params,
                                            String type) throws InvalidArgumentsException {

        clinicalVariableFactory.createClinicalVariable params, type
    }
}
