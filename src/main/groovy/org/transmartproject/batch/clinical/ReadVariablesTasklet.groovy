package org.transmartproject.batch.clinical

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.batch.model.Variable
import org.transmartproject.batch.support.Keys

/**
 *
 */
class ReadVariablesTasklet implements Tasklet {

    @Autowired
    ClinicalJobContext jobContext

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        String path = jobContext.jobParameters.get(Keys.COLUMN_MAP_FILE)
        if (!path) {
            throw new IllegalArgumentException("Not job parameter $Keys.COLUMN_MAP_FILE defined")
        }
        File file = new File(path)
        List<Variable> list = Variable.READER.apply(file)
        Set<File> dataFiles = Variable.getDataFiles(file.getParentFile(), list)
        Variable.validateDataFiles(dataFiles)
        //cjc.dataFileColumnsMap = dataFiles.collectEntries { [(it): null] }  //set the data files (no columns yet)
        println("read $list")
        jobContext.variables.clear()
        jobContext.variables.addAll(list)
        return RepeatStatus.FINISHED
    }

}
