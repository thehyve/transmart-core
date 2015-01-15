package org.transmartproject.batch.highdim.mrna.data.mapping

import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.StepExecutionListener
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.concept.GatherCurrentConceptsTasklet
import org.transmartproject.batch.highdim.platform.PlatformJobContextKeys

/**
 * Reads the platform and concepts from {@link MrnaMappings} and concepts and
 * writes them in the execution context.
 * This is need for other beans that assume such keys to be there.
 */
@Component
@JobScope
class MrnaMappingContextPromoterListener implements StepExecutionListener {

    @Autowired
    MrnaMappings mrnaMappings

    @Value('#{jobExecution.executionContext}')
    ExecutionContext executionContext

    @Override
    void beforeStep(StepExecution stepExecution) {}

    @Override
    ExitStatus afterStep(StepExecution stepExecution) {
        executionContext.putString(PlatformJobContextKeys.PLATFORM,
                mrnaMappings.platform)
        executionContext.put(GatherCurrentConceptsTasklet.LIST_OF_CONCEPTS_KEY,
                mrnaMappings.allConceptPaths)
    }
}
