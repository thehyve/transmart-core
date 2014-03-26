package org.transmartproject.db.clinical

import com.google.common.collect.Maps
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.clinical.ClinicalDataResource
import org.transmartproject.core.dataquery.clinical.ClinicalVariable
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn
import org.transmartproject.core.dataquery.clinical.PatientRow
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.dataquery.clinical.ClinicalDataTabularResult
import org.transmartproject.db.dataquery.clinical.TerminalConceptVariablesDataQuery
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable
import org.transmartproject.db.dataquery.highdim.parameterproducers.BindingUtils
import org.transmartproject.db.querytool.QueriesResourceService

class ClinicalDataResourceService implements ClinicalDataResource {

    static transactional = false

    def sessionFactory

    QueriesResourceService queriesResourceService;

    @Override
    ClinicalDataTabularResult retrieveData(List<QueryResult> queryResults,
                                           List<ClinicalVariable> variables) {
        Set<Patient> patients = queriesResourceService.getPatients(queryResults)
        retrieveDataForPatients(patients, variables)
    }

    @Override
    ClinicalDataTabularResult retrieveData(Set<Patient> patients, Set<OntologyTerm> ontologyTerms) {
        def allOntologyTerms = (ontologyTerms*.allDescendants).flatten()
        allOntologyTerms.addAll(ontologyTerms)
        def clinicalVariables =
                allOntologyTerms.findAll {
                    OntologyTerm.VisualAttributes.LEAF in it.visualAttributes
                }.collect {
                    createClinicalVariable(['concept_path': it.fullName],
                            ClinicalVariable.TERMINAL_CONCEPT_VARIABLE)
                }

        retrieveDataForPatients(patients, clinicalVariables)
    }

    ClinicalDataTabularResult retrieveDataForPatients(Collection<Patient> patientCollection, List<ClinicalVariable> variables) {

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
