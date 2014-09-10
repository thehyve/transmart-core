package org.transmartproject.batch.clinical

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.transmartproject.batch.model.Variable
import org.transmartproject.batch.support.LineListener
import org.transmartproject.batch.support.LineStepContributionAdapter

/**
 * Tasklet that reads the column map file (variables) and updates the ClinicalJobContext
 */
class ReadVariablesTasklet implements Tasklet {

    @Autowired
    ClinicalJobContext jobContext

    @Value("#{jobParameters['dataLocation']}")
    String dataLocation

    @Value("#{jobParameters['columnMapFile']}")
    String columnMapFile

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        File file = getFile()
        LineListener listener = new LineStepContributionAdapter(contribution)

        List<Variable> list = Variable.parse(file.newInputStream(), listener)
        jobContext.variables.clear()
        jobContext.variables.addAll(list)

        return RepeatStatus.FINISHED
    }

    File getFile() {
        if (!dataLocation) {
            throw new IllegalArgumentException('Data location not defined')
        }
        if (!columnMapFile) {
            throw new IllegalArgumentException('Column map file not defined')
        }
        new File(dataLocation, columnMapFile)
    }

}
