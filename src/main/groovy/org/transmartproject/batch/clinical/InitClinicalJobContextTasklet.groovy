package org.transmartproject.batch.clinical

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired

/**
 *
 */
class InitClinicalJobContextTasklet implements Tasklet {

    @Autowired
    ClinicalJobContext clinicalJobContext

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        def execContext = chunkContext.stepContext.stepExecution.jobExecution.executionContext
        clinicalJobContext.jobExecutionContext = execContext
        return RepeatStatus.FINISHED
    }
}
