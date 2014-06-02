package org.transmartproject.db.dataquery.clinical

import com.google.common.base.Function
import com.google.common.collect.AbstractIterator
import com.google.common.collect.Iterators
import com.google.common.collect.PeekingIterator
import groovy.transform.CompileStatic
import org.hibernate.ScrollableResults
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn
import org.transmartproject.core.dataquery.clinical.PatientRow
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.db.dataquery.clinical.variables.TerminalConceptVariable

@CompileStatic
class ClinicalDataTabularResult implements TabularResult<ClinicalVariableColumn, PatientRow> {

    private TerminalConceptVariablesTabularResult conceptVariablesTabularResult

    private SortedMap<Long, Patient> patientMap

    ClinicalDataTabularResult(ScrollableResults results,
                              List<ClinicalVariableColumn> indicesList,
                              List<TerminalConceptVariable> flattenedIndices,
                              SortedMap<Long, Patient> patientMap) {
        conceptVariablesTabularResult =
                new TerminalConceptVariablesTabularResult(
                        results, flattenedIndices)

        this.indicesList = indicesList
        this.patientMap = patientMap
        this.flattenedIndices = flattenedIndices
    }

    List<ClinicalVariableColumn> indicesList
    List<TerminalConceptVariable> flattenedIndices

    @Override
    Iterator<PatientRow> getRows() {
        def idToPatientEntries = patientMap.entrySet().iterator()
        PeekingIterator<PatientRow> conceptVariableRows =
                Iterators.peekingIterator(wrappedPatientRowsIterator())


        //XXX: we don't care about the patient part actually
        //     the plain implementation of DataRow, which is what's needed here
        //     should be moved to a different class
        // the empty data row should have the same indices that the normal rows, and so based on flattened indices
        def emptyDataRow = createEmptyRow(flattenedIndices)

        /* patients with no data will not be in the result set returned by
         * conceptVariablesTabularResult, so we need to add empty rows
         * for those patients. Both patientMap and conceptVariablesTabularResult
         * should return results sorted by patient id */
            new AbstractIterator<PatientRow>() {

                @Override
                protected PatientRow computeNext() {
                    if (!idToPatientEntries.hasNext()) {
                        if (conceptVariableRows.hasNext()) {
                            throw new UnexpectedResultException(
                                    'Found end of patient list before end of data result set')
                        }
                        endOfData()
                        return
                    }

                    Patient currentPatient = idToPatientEntries.next().value
                    if (!conceptVariableRows.hasNext() ||
                            currentPatient.id != conceptVariableRows.peek().patient.id) {
                        //empty row
                        new PatientRowImpl(
                                patient: currentPatient,
                                delegatingDataRow: emptyDataRow)
                    } else {
                        conceptVariableRows.next()
                    }
                }
            }
    }

    private DataRow createEmptyRow(List<TerminalConceptVariable> indices) {
        int i = 0

        new PatientIdAnnotatedDataRow(
                data: indices.collect { null },
                columnToIndex: indices.collectEntries { [it, i++] })
    }

    /**
     * Returns an iterator where each PatientRows element was wrapped in a PatientRowImpl.
     * The PatientRowImpl is AbstractComposedVariable-aware and implements properly the method getAt
     * @return
     */
    private Iterator<PatientRow> wrappedPatientRowsIterator() {
        /* TerminalConceptVariablesTabularResult returns a different
         * type of row, that doesn't implement PatientRow.
         * Adapt it to a PatientRow */
        Iterators.transform(
                conceptVariablesTabularResult.iterator(),
                { PatientIdAnnotatedDataRow originalRow ->
                    Patient patient = patientMap[originalRow.patientId]
                    if (!patient) {
                        throw new UnexpectedResultException(
                                "Found data for unexpected patient " +
                                        "(id $originalRow.patientId)")
                    }

                    new PatientRowImpl(
                            patient: patient,
                            delegatingDataRow: originalRow)
                } as Function<PatientIdAnnotatedDataRow, PatientRow>)
    }

    @Override
    Iterator<PatientRow> iterator() {
        rows
    }

    /* the rest, completely delegate to conceptVariablesTabularResult */

    @Override
    String getColumnsDimensionLabel() {
        conceptVariablesTabularResult.columnsDimensionLabel
    }

    @Override
    String getRowsDimensionLabel() {
        conceptVariablesTabularResult.rowsDimensionLabel
    }

    @Override
    void close() throws IOException {
        conceptVariablesTabularResult.close()
    }
}
