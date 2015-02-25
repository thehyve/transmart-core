package org.transmartproject.batch.highdim.datastd

import groovy.transform.CompileStatic
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.ParseException
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.transmartproject.batch.batchartifacts.AbstractSplittingItemReader
import org.transmartproject.batch.highdim.assays.AssayMappingsRowStore
import org.transmartproject.batch.highdim.assays.MappingFileRow
import org.transmartproject.batch.patient.Patient
import org.transmartproject.batch.patient.PatientSet

/**
 * Splits a standard data file row into several {@link StandardDataValue}s.
 * Needs to have the delegate set manually (no autowiring).
 *
 * Should be step-scoped.
 */
@CompileStatic
class StandardDataRowSplitterReader extends AbstractSplittingItemReader<StandardDataValue> {

    @Autowired
    private AssayMappingsRowStore assayMappings

    @Autowired
    private PatientSet patientSet

    @Value('#{stepExecution.executionContext}')
    private ExecutionContext stepExecutionContext

    @Lazy
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private List<String> columnNames = (List<String>) ({
        stepExecutionContext.get('tsv.header')
    })()

    private String lastAnnotationName

    // to be configured
    // to avoid instantiating a new object in the processor to convert
    // from standard to triple data
    Class<? extends StandardDataValue> dataPointClass = StandardDataValue

    @Lazy
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private List<Patient> patients = (List<Patient>) ({ ->
        columnNames.subList(1, columnNames.size()).collect { String sampleName ->
            MappingFileRow mapping = assayMappings.getBySampleName(sampleName)
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
                    "${patients.size() + 1} fields (ID_REF plus " +
                    "${patients.size()} patients/assays), " +
                    "got ${result.fieldCount}")
        }

        result
    }

    @Override
    protected StandardDataValue doRead() {
        if (position == 0) {
            // skip position 0
            position++
            lastAnnotationName = null
            return doRead()
        }

        if (lastAnnotationName == null) {
            lastAnnotationName = currentFieldSet.readString(0)
        }

        // has to be provided
        Double value = currentFieldSet.readDouble(position)

        dataPointClass.newInstance(
                patient: patients[position - 1],
                annotation: lastAnnotationName,
                value: value)
    }

    void setDelegate(ItemStreamReader<FieldSet> innerReader) {
        super.delegate = innerReader
    }
}
