package org.transmartproject.db.dataquery.clinical

import com.google.common.collect.HashMultiset
import com.google.common.collect.Maps
import com.google.common.collect.Multiset
import org.hibernate.Query
import org.hibernate.ScrollMode
import org.hibernate.ScrollableResults
import org.hibernate.engine.SessionImplementor
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable
import org.transmartproject.db.i2b2data.ConceptDimension

class TerminalConceptVariablesDataQuery {

    private static final int FETCH_SIZE = 10000

    Collection<TerminalConceptVariable> clinicalVariables
    Collection<Long> patientIds

    SessionImplementor session

    private boolean inited

    void init() {
        fillInTerminalConceptVariables()
        inited = true
    }

    ScrollableResults openResultSet() {
        if (!inited) {
            throw new IllegalStateException('init() not called successfully yet')
        }

        def conceptCodes = clinicalVariables*.conceptCode

        // see TerminalConceptVariable constants
        // see ObservationFact
        Query query = session.createQuery """
                SELECT
                    patient.id,
                    conceptCode,
                    valueType,
                    textValue,
                    numberValue
                FROM ObservationFact fact
                WHERE
                    patient.id IN (:patientIds)
                AND
                    fact.conceptCode IN (:conceptCodes)
                ORDER BY
                    patient ASC,
                    conceptCode ASC"""

        query.cacheable = false
        query.readOnly  = true
        query.fetchSize = FETCH_SIZE

        query.setParameterList 'patientIds', patientIds
        query.setParameterList 'conceptCodes', conceptCodes

        query.scroll ScrollMode.FORWARD_ONLY
    }

    private void fillInTerminalConceptVariables() {
        Map<String, TerminalConceptVariable> conceptPaths = Maps.newHashMap()
        Map<String, TerminalConceptVariable> conceptCodes = Maps.newHashMap()

        if (!clinicalVariables && !patientIds) {
            throw new InvalidArgumentsException('Either clinical variable or patient set should be specified')
        }

        clinicalVariables.each { TerminalConceptVariable it ->
            if (!(it instanceof TerminalConceptVariable)) {
                throw new InvalidArgumentsException(
                        'Only terminal concept variables are supported')
            }

            if (it.conceptCode) {
                if (conceptCodes.containsKey(it.conceptCode)) {
                    throw new InvalidArgumentsException("Specified multiple " +
                            "variables with the same concept code: " +
                            it.conceptCode)
                }
                conceptCodes[it.conceptCode] = it
            } else if (it.conceptPath) {
                if (conceptPaths.containsKey(it.conceptPath)) {
                    throw new InvalidArgumentsException("Specified multiple " +
                            "variables with the same concept path: " +
                            it.conceptPath)
                }
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
                if (conceptPaths.keySet()) {
                    'in' 'conceptPath', conceptPaths.keySet()
                }
                if (conceptCodes.keySet()) {
                    'in' 'conceptCode', conceptCodes.keySet()
                }
            }
        }

        for (concept in res) {
            String conceptPath = concept[0],
                   conceptCode = concept[1]

            if (conceptPaths[conceptPath]) {
                TerminalConceptVariable variable = conceptPaths[conceptPath]
                variable.conceptCode = conceptCode
            }
            if (conceptCodes[conceptCode]) {
                TerminalConceptVariable variable = conceptCodes[conceptCode]
                variable.conceptPath = conceptPath
            }
            // if both ifs manage we have the variable repeated (specified once
            // with concept code and once with concept path), and we'll catch
            // that further down
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
                        "'${var.conceptCode}' did not yield any results")
            }
        }

        Multiset multiset = HashMultiset.create clinicalVariables
        if (multiset.elementSet().size() < clinicalVariables.size()) {
            throw new InvalidArgumentsException("Repeated variables in the " +
                    "query (though once their concept path was specified and " +
                    "on the second time their concept code was specified): " +
                    multiset.elementSet().findAll {
                            multiset.count(it) > 1
                    })
        }

    }

}
