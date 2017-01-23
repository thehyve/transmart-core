package org.transmartproject.batch.highdim.mirna.data

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ExecutionContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.highdim.assays.SaveAssayIdListener

/**
 * Account for values stored in the job execution context.
 */

@JobScope
@Component
class MirnaDataJobContextItems {

    @Value('#{jobExecution.executionContext}')
    ExecutionContext jobExecutionContext

    Map<String, Long> getSampleCodeAssayIdMap() {
        jobExecutionContext.get(SaveAssayIdListener.MAPPINGS_CONTEXT_KEY)
    }
}
