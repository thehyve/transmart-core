package org.transmartproject.batch.clinical

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.batch.model.WordMapping
import org.transmartproject.batch.support.Keys

/**
 *
 */
class ReadWordMapTasklet implements Tasklet {

    @Autowired
    ClinicalJobContext jobContext

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        String path = jobContext.jobParameters.get(Keys.WORD_MAP_FILE)
        List<WordMapping> list
        if (path) {
            File file = new File(path)
            list = WordMapping.READER.apply(file)
            println("read $list")
            list.each {
                if (it.newValue == 'null') {
                    it.newValue = null //we want the value null, not the string 'null'
                }
            }

        } else {
            list = []
        }

        jobContext.wordMappings.clear()
        jobContext.wordMappings.addAll(list)
        //jobContext.wordMappingsPerFile = WordMapping.getMappings(result)

        return RepeatStatus.FINISHED
    }

}
