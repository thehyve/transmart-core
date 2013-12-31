package org.transmartproject.db.dataquery.clinical

import com.google.common.collect.Maps
import org.hibernate.Query
import org.hibernate.ScrollMode
import org.hibernate.ScrollableResults
import org.hibernate.engine.SessionImplementor
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.i2b2data.PatientDimension

class TerminalConceptVariablesDataQuery {

    private static final int FETCH_SIZE = 10000

    List<TerminalConceptVariable> clinicalVariables

    List<QueryResult> resultInstances

    SessionImplementor session

    private boolean inited

    void init() {
        fillInTerminalConceptVariables()
        inited = true
    }

    Map<Long, Patient> fetchPatientMap() {
        /* This will load all the patients in memory
         * If this turns out to be a bad strategy, two alternatives are
         * possible:
         * 1) run the two queries side by side, both ordered by patient id
         * 2) join the patient table in the data query and build the patient
         *   from the data returned there.
         */
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

        def result = Maps.newTreeMap()
        ScrollableResults results = query.scroll ScrollMode.FORWARD_ONLY
        while (results.next()) {
            PatientDimension patient = results.get()[0]
            result[patient.id] = patient
        }

        result
    }

    ScrollableResults openResultSet() {
        if (!inited) {
            throw new IllegalStateException('init() not called successfully yet')
        }

        // see TerminalConceptVariable constants
        // see ObservationFact
        Query query = session.createQuery '''
                SELECT
                    patient.id,
                    conceptCode,
                    valueType,
                    textValue,
                    numberValue
                FROM ObservationFact fact
                WHERE
                    patient.id IN (
                        SELECT pset.patient.id
                        FROM QtPatientSetCollection pset
                        WHERE pset.resultInstance IN (:queryResults))
                    AND fact.conceptCode IN (:conceptCodes)
                ORDER BY
                    patient ASC,
                    conceptCode ASC'''

        query.cacheable = false
        query.readOnly  = true
        query.fetchSize = FETCH_SIZE

        query.setParameterList 'queryResults',  resultInstances
        query.setParameterList 'conceptCodes', clinicalVariables*.conceptCode

        query.scroll ScrollMode.FORWARD_ONLY
    }

    private void fillInTerminalConceptVariables() {
        Map<String, TerminalConceptVariable> conceptPaths = Maps.newHashMap()
        Map<String, TerminalConceptVariable> conceptCodes = Maps.newHashMap()

        if (!clinicalVariables) {
            throw new InvalidArgumentsException('No clinical variables specified')
        }

        clinicalVariables.each { TerminalConceptVariable it ->
            if (!(it instanceof TerminalConceptVariable)) {
                throw new InvalidArgumentsException(
                        'Only terminal concept variables are supported')
            }

            if (it.conceptCode) {
                conceptCodes[it.conceptCode] = it
            } else if (it.conceptPath) {
                conceptPaths[it.conceptPath] = it
            }
        }

        // find the concepts
        def res = ConceptDimension.withCriteria {
            projections {
                property 'conceptPath'
                property 'conceptCode'
            }

            or {
                'in' 'conceptPath', conceptPaths.keySet()
                'in' 'conceptCode', conceptCodes.keySet()
            }
        }

        for (concept in res) {
            String conceptPath = concept[0],
                   conceptCode = concept[1]

            if (conceptPaths[conceptPath]) {
                TerminalConceptVariable variable = conceptPaths[conceptPath]
                variable.conceptCode = conceptCode
            } else { // if (conceptCodes[conceptCode])
                TerminalConceptVariable variable = conceptCodes[conceptCode]
                variable.conceptPath = conceptPath
            }
        }

        // check we found all the concepts
        for (var in conceptPaths.values()) {
            if (var.conceptCode == null) {
                throw new InvalidArgumentsException("Concept path " +
                        "'${var.conceptPath}' did not yield any results")
            }
        }
        for (var in conceptCodes.values()) {
            if (var.conceptPath == null) {
                throw new InvalidArgumentsException("Concept code " +
                        "'${var.conceptCode} did not yield any results")
            }
        }

    }

}
