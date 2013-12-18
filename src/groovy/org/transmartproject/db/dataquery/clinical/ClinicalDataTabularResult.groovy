package org.transmartproject.db.dataquery.clinical

import com.google.common.base.Function
import com.google.common.collect.AbstractIterator
import com.google.common.collect.Iterators
import com.google.common.collect.PeekingIterator
import groovy.transform.CompileStatic
import org.hibernate.ScrollableResults
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn
import org.transmartproject.core.dataquery.clinical.PatientRow
import org.transmartproject.core.exceptions.UnexpectedResultException

@CompileStatic
class ClinicalDataTabularResult implements TabularResult<ClinicalVariableColumn, PatientRow> {

    /*
     * Right now, we only support TerminalConceptVariables, so we can delegate
     * most stuff exclusively to the conceptVariablesTabularResult
     */

    private TerminalConceptVariablesTabularResult conceptVariablesTabularResult

    private SortedMap<Long, Patient> patientMap

    ClinicalDataTabularResult(ScrollableResults results,
                              List<ClinicalVariableColumn> indicesList,
                              SortedMap<Long, Patient> patientMap) {
        conceptVariablesTabularResult =
                new TerminalConceptVariablesTabularResult(results, indicesList)

        this.indicesList = indicesList
        this.patientMap = patientMap
    }

    List<ClinicalVariableColumn> indicesList

    @Override
    Iterator<PatientRow> getRows() {
        def patientMapEntries = patientMap.entrySet().iterator()
        PeekingIterator<PatientRow> conceptVariableRows =
                Iterators.peekingIterator(transformedConceptVariableRows())

        //XXX: we don't care about the patient part actually
        //     the plain implementation of DataRow, which is what's needed here
        //     should be moved to a different class
        int i = 0
        def emptyDataRow = new PatientIdAnnotatedDataRow(
                data: indicesList.collect { null },
                columnToIndex: indicesList.collectEntries { [it, i++] })

        /* patients with no data will not be in the result set returned by
         * conceptVariablesTabularResult, so we need to add empty rows
         * for those patients. Both patientMap and conceptVariablesTabularResult
         * should return results sorted by patient id */
        new AbstractIterator<PatientRow>() {

            @Override
            protected PatientRow computeNext() {
                if (!patientMapEntries.hasNext()) {
                    if (conceptVariableRows.hasNext()) {
                        throw new UnexpectedResultException(
                                'Found end of patient list before end of data result set')
                    }
                    endOfData()
                    return
                }

                Patient currentPatient = patientMapEntries.next().value
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

    private Iterator<PatientRow> transformedConceptVariableRows() {
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
