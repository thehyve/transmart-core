package org.transmartproject.db.clinical

import com.google.common.collect.Maps
import org.hibernate.Query
import org.hibernate.ScrollMode
import org.hibernate.ScrollableResults
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.clinical.ClinicalDataResource
import org.transmartproject.core.dataquery.clinical.ClinicalVariable
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn
import org.transmartproject.core.dataquery.clinical.PatientRow
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.Study
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.dataquery.clinical.ClinicalDataTabularResult
import org.transmartproject.db.dataquery.clinical.TerminalConceptVariablesDataQuery
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable
import org.transmartproject.db.dataquery.highdim.parameterproducers.BindingUtils

class ClinicalDataResourceService implements ClinicalDataResource {

    def sessionFactory

    @Override
    ClinicalDataTabularResult retrieveData(List<QueryResult> queryResults,
                                           List<ClinicalVariable> variables) {
        retrieveData(fetchPatients(queryResults), variables)
    }

    ClinicalDataTabularResult retrieveData(Study study, List<Patient> patients, List<OntologyTerm> ontologyTerms) {
        def ontologyTermsToUse = ontologyTerms ?: [ study.ontologyTerm ]
        def descendants = (ontologyTermsToUse*.allDescendants).flatten()
        def clinicalVariables =
                descendants.findAll {
                    OntologyTerm.VisualAttributes.LEAF in it.visualAttributes
                }.collect {
                    createClinicalVariable(['concept_path': it.fullName],
                            ClinicalVariable.TERMINAL_CONCEPT_VARIABLE)
                }

        retrieveDataNew(patients ?: study.getPatients(), clinicalVariables)
    }

    ClinicalDataTabularResult retrieveDataNew(Collection<Patient> patientCollection, List<ClinicalVariable> variables) {

        def session = sessionFactory.openStatelessSession()

        try {
            def patientMap = Maps.newTreeMap()

            patientCollection.each { patientMap[it.id] = it }

            TerminalConceptVariablesDataQuery query =
                    new TerminalConceptVariablesDataQuery(
                            session: session,
                            patientIds: patientMap.keySet(),
                            clinicalVariables: variables)
            query.init()

            new ClinicalDataTabularResult(
                    query.openResultSet(),
                    variables,
                    patientMap)
        } catch (Throwable t) {
            session.close()
            throw t
        }
    }

    List<Patient> fetchPatients(List<QueryResult> resultInstances) {
        /* This will load all the patients in memory
         * If this turns out to be a bad strategy, two alternatives are
         * possible:
         * 1) run the two queries side by side, both ordered by patient id
         * 2) join the patient table in the data query and build the patient
         *   from the data returned there.
         */
        def session = sessionFactory.openStatelessSession()

        Query query = session.createQuery '''
                FROM PatientDimension p
                WHERE
                    p.id IN (
                        SELECT pset.patient.id
                        FROM QtPatientSetCollection pset
                        WHERE pset.resultInstance IN (:queryResults))
                ORDER BY p ASC'''

        query.cacheable = false
        query.readOnly  = true
        query.setParameterList 'queryResults', resultInstances

        def result = []
        ScrollableResults results = query.scroll ScrollMode.FORWARD_ONLY
        while (results.next()) {
            result << results.get()[0]
        }

        result
    }

    @Override
    TabularResult<ClinicalVariableColumn, PatientRow> retrieveData(QueryResult patientSet,
                                                                   List<ClinicalVariable> variables) {
        retrieveData([patientSet], variables)
    }

    @Override
    ClinicalVariable createClinicalVariable(Map<String, Object> params,
                                            String type) throws InvalidArgumentsException {

        // only TERMINAL_CONCEPT_VARIABLE supported, let's implement it inline
        if (type != ClinicalVariable.TERMINAL_CONCEPT_VARIABLE) {
            throw new InvalidArgumentsException("Invalid clinical variable " +
                    "type '$type', only " +
                    "${ClinicalVariable.TERMINAL_CONCEPT_VARIABLE} is supported")
        }

        if (params.size() != 1) {
            throw new InvalidArgumentsException("Expected exactly one parameter, " +
                    "got ${params.keySet()}")
        }

        if (params['concept_code']) {
            String conceptCode =
                    BindingUtils.getParam params, 'concept_code', String
            new TerminalConceptVariable(conceptCode: conceptCode)
        } else if (params['concept_path']) {
            String conceptPath =
                    BindingUtils.getParam params, 'concept_path', String
            new TerminalConceptVariable(conceptPath: conceptPath)
        } else {
            throw new InvalidArgumentsException("Expected the given parameter " +
                    "to be one of 'concept_code', 'concept_path', got " +
                    "'${params.keySet().iterator().next()}'")
        }
    }
}
