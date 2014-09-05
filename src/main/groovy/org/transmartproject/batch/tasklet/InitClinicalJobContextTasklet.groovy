package org.transmartproject.batch.tasklet

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.transmartproject.batch.Keys
import org.transmartproject.batch.model.ClinicalJobContext

/**
 *
 */
class InitClinicalJobContextTasklet implements Tasklet {

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        Map args = [
                columnMapFile: getFileArg(chunkContext, Keys.COLUMN_MAP_FILE, true),
                wordMapFile: getFileArg(chunkContext, Keys.WORD_MAP_FILE, false),
        ]

        ClinicalJobContext ctx = new ClinicalJobContext(args)

        chunkContext.stepContext.stepExecution.jobExecution.executionContext.put(Keys.CLINICAL_JOB_CONTEXT, ctx)
        return RepeatStatus.FINISHED
    }

    private File getFileArg(ChunkContext context, String jobParam, boolean mandatory) {
        String path = context.stepContext.getJobParameters().get(jobParam)
        if (!path && mandatory) {
            throw new IllegalArgumentException("No job parameter $jobParam")
        }

        File result
        if (path) {
            result = new File(path)
            if (!result.exists()) {
                throw new IllegalArgumentException("File $path not found, defined in job parameter $jobParam")
            }
        }
        result
    }

}
