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

import org.hibernate.engine.SessionImplementor
import org.springframework.stereotype.Component
import org.transmartproject.db.dataquery.clinical.variables.AcrossTrialsTerminalVariable
import org.transmartproject.db.dataquery.clinical.variables.TerminalClinicalVariable
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable
import org.transmartproject.db.i2b2data.PatientDimension

@Component /* not scanned; explicit bean definition */
class InnerClinicalTabularResultFactory {

    public Collection<TerminalClinicalVariablesTabularResult> createIntermediateResults(
            SessionImplementor session,
            Iterable<PatientDimension> patients,
            List<TerminalClinicalVariable> flattenedVariables) {
        flattenedVariables.groupBy { it.group }.
                collect { group, variables ->
                    createForGroup group, session, patients, variables
                }
    }

    public TerminalClinicalVariablesTabularResult createForGroup(
            String group,
            SessionImplementor session,
            Iterable<PatientDimension> patients,
            List<TerminalClinicalVariable> relevantVariables) {
        switch (group) {
            case TerminalConceptVariable.GROUP_NAME:
                def query = new TerminalConceptVariablesDataQuery(
                        session:           session,
                        patients:          patients,
                        clinicalVariables: relevantVariables)
                query.init()

                return new TerminalClinicalVariablesTabularResult(
                                query.openResultSet(), relevantVariables)
            case AcrossTrialsTerminalVariable.GROUP_NAME:
                def query = new AcrossTrialsDataQuery(
                        session:           session,
                        patients:          patients,
                        clinicalVariables: relevantVariables)
                query.init()

                return new TerminalClinicalVariablesTabularResult(
                        query.openResultSet(), relevantVariables)
            default:
                throw new IllegalArgumentException("Unknown group name: $group")
        }
    }
}
