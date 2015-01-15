package org.transmartproject.batch.highdim.assays

import com.google.common.collect.Maps
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.AfterStep
import org.springframework.batch.core.annotation.AfterWrite
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ExecutionContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Listener that saves associations between patient ids and assay ids in the
 * context (after each write in the step context and after the step in the
 * job context).
 */
@Component
@StepScope
class SaveAssayIdListener {

    public static final MAPPINGS_CONTEXT_KEY = 'patientIdAssayIdMap'

    @Value('#{stepExecution.executionContext}')
    ExecutionContext stepExecutionContext

    @Value('#{stepExecution.jobExecution.executionContext}')
    ExecutionContext jobExecutionContext

    @AfterStep
    @SuppressWarnings('UnusedMethodParameter')
    ExitStatus afterStep(StepExecution stepExecution) {
        jobExecutionContext.put(MAPPINGS_CONTEXT_KEY, patientIdAssayIdMap)
    }

    @AfterWrite
    void afterWrite(List<? extends Assay> items) {
        def assayMappings = patientIdAssayIdMap
        items.each { Assay assay ->
            assayMappings.put assay.patient.id, assay.id
        }
        stepExecutionContext.put(MAPPINGS_CONTEXT_KEY, assayMappings)
    }

    Map<String, Long> getPatientIdAssayIdMap() {
        stepExecutionContext.get(MAPPINGS_CONTEXT_KEY) ?:
                Maps.newHashMap()
    }
}
