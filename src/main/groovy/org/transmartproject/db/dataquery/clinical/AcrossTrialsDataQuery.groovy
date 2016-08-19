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

package org.transmartproject.db.dataquery.clinical

import com.google.common.collect.Lists
import com.google.common.collect.Maps
import org.hibernate.ScrollMode
import org.hibernate.ScrollableResults
import org.hibernate.engine.SessionImplementor
import org.transmartproject.core.dataquery.clinical.ClinicalVariable
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.clinical.variables.AcrossTrialsTerminalVariable
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.ontology.ModifierDimensionView

import static org.transmartproject.db.ontology.AbstractAcrossTrialsOntologyTerm.ACROSS_TRIALS_TOP_TERM_NAME
import static org.transmartproject.db.util.GormWorkarounds.createCriteriaBuilder
import static org.transmartproject.db.util.GormWorkarounds.getHibernateInCriterion

/**
 * Across trials counterpart of {@link TerminalConceptVariablesDataQuery}.
 */
class AcrossTrialsDataQuery {

    List<AcrossTrialsTerminalVariable> clinicalVariables

    Iterable<PatientDimension> patients

    SessionImplementor session

    private boolean inited

    void init() {
        fillInAcrossTrialsTerminalVariables()
        inited = true
    }

    ScrollableResults openResultSet() {
        if (!inited) {
            throw new IllegalStateException('init() not called successfully yet')
        }

        def criteriaBuilder = createCriteriaBuilder(ObservationFact, 'obs', session)
        criteriaBuilder.with {
            projections {
                property 'patient.id'
                property 'modifierCd'
                property 'valueType'
                property 'textValue'
                property 'numberValue'
            }
            order 'patient.id'
            order 'modifierCd'
        }

        if (patients instanceof PatientQuery) {
            criteriaBuilder.add(getHibernateInCriterion('patient.id',
                    patients.forIds()))
        } else {
            criteriaBuilder.in('patient',  Lists.newArrayList(patients))
        }

        criteriaBuilder.in('modifierCd', clinicalVariables*.code)

        criteriaBuilder.scroll ScrollMode.FORWARD_ONLY
    }

    private void fillInAcrossTrialsTerminalVariables() {
        Map<String, AcrossTrialsTerminalVariable> conceptPaths = Maps.newHashMap()

        if (!clinicalVariables) {
            throw new InvalidArgumentsException('No clinical variables specified')
        }

        clinicalVariables.each { ClinicalVariable it ->
            if (!(it instanceof AcrossTrialsTerminalVariable)) {
                throw new InvalidArgumentsException(
                        'Only across trial terminal variables are supported')
            }

            if (!it.modifierCode) {
                if (conceptPaths.containsKey(it.conceptPath)) {
                    throw new InvalidArgumentsException("Specified multiple " +
                            "variables with the same concept path: " +
                            it.conceptPath)
                }
                conceptPaths[convertPath(it.conceptPath)] = it
            }
        }

        def res = ModifierDimensionView.withCriteria {
            projections {
                property 'path'
                property 'code'
            }

            'in' 'path', conceptPaths.keySet()
        }

        for (modifier in res) {
            String path = modifier[0],
                   code = modifier[1]

            AcrossTrialsTerminalVariable variable = conceptPaths[path]
            variable.modifierCode = code
        }

        for (var in conceptPaths.values()) {
            if (var.modifierCode == null) {
                throw new InvalidArgumentsException("Concept path " +
                        "'${var.conceptPath}' did not yield any results")
            }
        }
    }

    private String convertPath(originalPath) {
        originalPath - ~/^\\${ACROSS_TRIALS_TOP_TERM_NAME}/
    }
}
