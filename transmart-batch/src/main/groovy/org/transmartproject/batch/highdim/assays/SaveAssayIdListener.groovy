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
 * Listener that saves associations between sample codes and assay ids in the
 * context (after each write in the step context and after the step in the
 * job context).
 */
@Component
@StepScope
class SaveAssayIdListener {

    public static final MAPPINGS_CONTEXT_KEY = 'sampleCodeAssayIdMap'

    @Value('#{stepExecution.executionContext}')
    ExecutionContext stepExecutionContext

    @Value('#{stepExecution.jobExecution.executionContext}')
    ExecutionContext jobExecutionContext

    @AfterStep // non-transactional
    @SuppressWarnings('UnusedMethodParameter')
    ExitStatus afterStep(StepExecution stepExecution) {
        jobExecutionContext.put(MAPPINGS_CONTEXT_KEY, sampleCodeAssayIdMap)
    }

    @AfterWrite // transactional
    void afterWrite(List<? extends Assay> items) {
        def assayMappings = sampleCodeAssayIdMap
        items.each { Assay assay ->
            assayMappings.put assay.sampleCode, assay.id
        }
        stepExecutionContext.put(MAPPINGS_CONTEXT_KEY, assayMappings)
    }

    Map<String, Long> getSampleCodeAssayIdMap() {
        stepExecutionContext.get(MAPPINGS_CONTEXT_KEY) ?:
                Maps.newHashMap()
    }
}
