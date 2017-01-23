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

import com.google.common.collect.*
import com.google.common.io.Closer
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.hibernate.engine.SessionImplementor
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.clinical.PatientRow
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.db.dataquery.clinical.variables.TerminalClinicalVariable

@CompileStatic
class ClinicalDataTabularResult implements TabularResult<TerminalClinicalVariable, PatientRow> {

    private Collection<TerminalClinicalVariablesTabularResult> tabResults

    private SortedMap<Long, Patient> patientMap

    private SessionImplementor session

    List<TerminalClinicalVariable> indicesList

    Map<TerminalClinicalVariable, TerminalClinicalVariablesTabularResult> indexOwnerMap

    @CompileStatic(TypeCheckingMode.PASS)
    ClinicalDataTabularResult(SessionImplementor session,
                              Collection<TerminalClinicalVariablesTabularResult> tabResults,
                              SortedMap<Long, Patient> patientMap) {
        this.session    = session
        this.patientMap = patientMap

        def indexOwnerBuilder  = ImmutableMap.builder()
        def indicesListBuilder = ImmutableList.builder()

        tabResults.each { TerminalClinicalVariablesTabularResult tabResult ->
            tabResult.indicesList.each { TerminalClinicalVariable variable ->
                indexOwnerBuilder.put variable, tabResult
                indicesListBuilder.add variable
            }
        }

        this.tabResults    = tabResults
        this.indexOwnerMap = indexOwnerBuilder.build()
        this.indicesList   = indicesListBuilder.build()
    }

    @Override
    Iterator<PatientRow> getRows() {
        new ClinicalDataJoinedIterator(this)
    }

    @Override
    Iterator<PatientRow> iterator() {
        rows
    }

    @Override
    String getColumnsDimensionLabel() {
        tabResults.first().columnsDimensionLabel
    }

    @Override
    String getRowsDimensionLabel() {
        tabResults.first().rowsDimensionLabel
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    void close() throws IOException {
        Closer closer = Closer.create()
        /* session must be last thing to be closed, so we have to add it to the
         * the closer first */
        closer.register(new Closeable() {
            void close() throws IOException {
                session.close()
            }
        })
        tabResults.each { closer.register it }
        closer.close()
    }
}

/** grails doesn't play well with inner classes */
@CompileStatic
class ClinicalDataJoinedIterator extends AbstractIterator<PatientRow> {

    ClinicalDataTabularResult outer

    Iterator<Map.Entry<Long, Patient>> idToPatientEntries =
            outer.patientMap.entrySet().iterator()

    List<PeekingIterator<PatientIdAnnotatedDataRow>> innerIterators = []

    Map<Iterator<PatientIdAnnotatedDataRow>, DataRow> emptyRows = [:]

    Map<Iterator<PatientIdAnnotatedDataRow>, String> groups = [:]

    @CompileStatic(TypeCheckingMode.SKIP)
    ClinicalDataJoinedIterator(ClinicalDataTabularResult outer) {
        this.outer = outer
        outer.tabResults.each { tr ->
            def newIterator = Iterators.peekingIterator(tr.iterator())
            innerIterators << newIterator
            emptyRows[newIterator] = createEmptyRow tr.indicesList
            groups[newIterator] = tr.variableGroup
        }
    }

    @Override
    protected PatientRow computeNext() {
        if (!idToPatientEntries.hasNext()) {
            if (innerIterators*.hasNext().any()) {
                throw new UnexpectedResultException(
                        'Found end of patient list before end of data result set')
            }
            return endOfData()
        }

        Patient currentPatient = idToPatientEntries.next().value

        /* patients with no data will not be in the result set returned by
         * conceptVariablesTabularResult, so we need to add empty rows
         * for those patients. Both patientMap and
         * conceptVariablesTabularResult should return results sorted by
         * patient id */

        Map delegatingDataRows = innerIterators.collectEntries {
            PeekingIterator<PatientIdAnnotatedDataRow> innerIt ->
                if (!innerIt.hasNext() ||
                        currentPatient.id != innerIt.peek().patientId) {
                    // empty row
                    [groups[innerIt], emptyRows[innerIt]]
                } else {
                    [groups[innerIt], innerIt.next()]
                }
        }

        new PatientRowImpl(
                patient: currentPatient,
                flattenedIndices: outer.indicesList,
                delegatingDataRows: delegatingDataRows)
    }

    private PatientIdAnnotatedDataRow createEmptyRow(
            List<TerminalClinicalVariable> indices) {
        int i = 0

        // we don't need to actually fill the patient
        // we actually use that fact to reuse the object
        new PatientIdAnnotatedDataRow(
                data: indices.collect { null },
                columnToIndex: indices.collectEntries { [it, i++] })
    }
}
