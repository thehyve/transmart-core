package org.transmartproject.batch.batchartifacts

import groovy.text.SimpleTemplateEngine
import groovy.util.logging.Slf4j
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.scope.context.JobContext
import org.springframework.batch.core.scope.context.JobSynchronizationManager
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus

/**
 * Logs an error and forces a failed state. Can resolve expressions against the
 * job context (ctx) and parameters (params).
 */
@Slf4j
class FailWithMessageTasklet implements Tasklet {

    private final String messageTemplate

    FailWithMessageTasklet(String messageTemplate) {
        this.messageTemplate = messageTemplate
    }

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        JobContext jobContext = JobSynchronizationManager.context

        SimpleTemplateEngine engine = new SimpleTemplateEngine()
        Map binding = [
                ctx   : jobContext.jobExecutionContext,
                params: jobContext.jobParameters,
        ]
        Writable w = engine.createTemplate(messageTemplate).make(binding)

        log.error("Failing job: $w")

        chunkContext.stepContext.stepExecution.exitStatus =
                new ExitStatus('FAILED', w.toString())

        RepeatStatus.FINISHED
    }
}
