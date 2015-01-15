package org.transmartproject.batch.highdim.mrna.data.pass

import groovy.transform.CompileStatic
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ParseException
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.transmartproject.batch.batchartifacts.AbstractSplittingItemReader
import org.transmartproject.batch.highdim.assays.MappingFileRow
import org.transmartproject.batch.highdim.mrna.data.mapping.MrnaMappings
import org.transmartproject.batch.patient.Patient
import org.transmartproject.batch.patient.PatientSet

/**
 * Spits an mrna data file row into several {@link MrnaDataValue}s.
 * Needs to have the delegate set.
 */
@CompileStatic
class MrnaDataRowSplitterReader extends AbstractSplittingItemReader<MrnaDataValue> {

    @Autowired
    private MrnaMappings mrnaMappings

    @Autowired
    private PatientSet patientSet

    @Value('#{stepExecution.executionContext}')
    private ExecutionContext stepExecutionContext

    @Lazy
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private List<String> columnNames = (List<String>) ({
        stepExecutionContext.get('tsv.header')
    })()

    private String lastProbeName

    @Lazy
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private List<Patient> patients = (List<Patient>) ({ ->
        columnNames.subList(1, columnNames.size()).collect { String sampleName ->
            MappingFileRow mapping = mrnaMappings.getBySampleName(sampleName)
            assert mapping != null
            Patient p = patientSet[mapping.subjectId]
            assert p != null
            p
        }
    })()

    @Override
    protected FieldSet fetchNextDelegateLine() {
        def result = super.fetchNextDelegateLine()
        if (result && result.fieldCount != patients.size() + 1) {
            throw new ParseException("Expected a row with " +
                    "${patients.size() + 1} fields, got ${result.fieldCount}")
        }

        result
    }

    @Override
    protected MrnaDataValue doRead() {
        if (position == 0) {
            // skip position 0
            position++
            lastProbeName = null
            return doRead()
        }

        if (lastProbeName == null) {
            lastProbeName = currentFieldSet.readString(0)
        }

        // has to be provided
        Double value = currentFieldSet.readDouble(position)

        new MrnaDataValue(
                patient: patients[position - 1],
                probe: lastProbeName,
                value: value)
    }
}
