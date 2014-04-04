package org.transmartproject.db.clinical

import com.google.common.collect.Maps
import com.google.common.collect.Sets
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.clinical.*
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.dataquery.clinical.ClinicalDataTabularResult
import org.transmartproject.db.dataquery.clinical.TerminalConceptVariablesDataQuery
import org.transmartproject.db.dataquery.clinical.variables.ClinicalVariableFactory
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable

class ClinicalDataResourceService implements ClinicalDataResource {

    static transactional = false

    def sessionFactory

    @Autowired
    ClinicalVariableFactory clinicalVariableFactory

    @Override
    ClinicalDataTabularResult retrieveData(List<QueryResult> queryResults,
                                           List<ClinicalVariable> variables) {
        Set<Patient> patients
        if (queryResults.size() == 1) {
            patients = queryResults[0].patients
        } else {
            patients = Sets.newTreeSet(
                    { a, b -> a.id <=> b.id } as Comparator)
            queryResults.each {
                patients.addAll it.patients
            }
        }

        retrieveData(patients, variables)
    }

    @Override
    TabularResult<ClinicalVariableColumn, PatientRow> retrieveData(Set<Patient> patientCollection,
                                                                   List<ClinicalVariable> variables) {

        def session = sessionFactory.openStatelessSession()

        try {
            def patientMap = Maps.newTreeMap()

            patientCollection.each { patientMap[it.id] = it }

            List<TerminalConceptVariable> flattenedVariables = []
            flattenClinicalVariables(flattenedVariables, variables)

            TerminalConceptVariablesDataQuery query =
                    new TerminalConceptVariablesDataQuery(
                            session: session,
                            patientIds: patientMap.keySet(),
                            clinicalVariables: flattenedVariables)
            query.init()

            new ClinicalDataTabularResult(
                    query.openResultSet(),
                    variables,
                    flattenedVariables,
                    patientMap)
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
        retrieveData([patientSet], variables)
    }

    @Override
    ClinicalVariable createClinicalVariable(Map<String, Object> params,
                                            String type) throws InvalidArgumentsException {

        clinicalVariableFactory.createClinicalVariable params, type
    }
}
