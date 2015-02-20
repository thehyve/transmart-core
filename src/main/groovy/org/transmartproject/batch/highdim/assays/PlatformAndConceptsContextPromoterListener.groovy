package org.transmartproject.batch.highdim.assays

import groovy.util.logging.Slf4j
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
 * Reads the platform and concepts from {@link MappingsFileRowStore} and concepts and
 * writes them in the execution context.
 * This is need for other beans that assume such keys to be there.
 */
@Component
@JobScope
@Slf4j
class PlatformAndConceptsContextPromoterListener implements StepExecutionListener {

    @Autowired
    AssayMappingsRowStore assayMappingsRowStore

    @Value('#{jobExecution.executionContext}')
    ExecutionContext executionContext

    @Override
    void beforeStep(StepExecution stepExecution) {}

    @SuppressWarnings('CatchException')
    @Override
    ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.exitStatus != ExitStatus.COMPLETED) {
            log.warn("Step status was ${stepExecution.exitStatus}; " +
                    "will not promote anything")
            return stepExecution.exitStatus
        }

        try {
            executionContext.putString(PlatformJobContextKeys.PLATFORM,
                    assayMappingsRowStore.platform)
            executionContext.put(GatherCurrentConceptsTasklet.LIST_OF_CONCEPTS_KEY,
                    assayMappingsRowStore.allConceptPaths)
            stepExecution.exitStatus
        } catch (Exception e) {
            log.error('Error in platform and concepts context promoter; ' +
                    'failing step', e)
            ExitStatus.FAILED
        }
    }
}
