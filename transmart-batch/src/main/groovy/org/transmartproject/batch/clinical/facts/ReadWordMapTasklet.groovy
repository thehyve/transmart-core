package org.transmartproject.batch.clinical.facts

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.transmartproject.batch.support.LineListener
import org.transmartproject.batch.support.LineStepContributionAdapter

import java.nio.file.Files
import java.nio.file.Path

/**
 * Tasklet that reads the word map file (if defined) and populates the word mappings list
 */
class ReadWordMapTasklet implements Tasklet {

    @Value("#{jobParameters['WORD_MAP_FILE']}")
    Path wordMapFile

    @Value("#{clinicalJobContext.wordMappings}")
    List<WordMapping> wordMappings

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        List<WordMapping> list
        if (wordMapFile) {
            LineListener listener = new LineStepContributionAdapter(contribution)
            wordMapFile.withInputStream {
                list = WordMapping.parse(Files.newInputStream(wordMapFile), listener)
            }
            /* TODO: should be in the mapper... */
            list.each {
                if (it.newValue == null || it.newValue == 'null') {
                    it.newValue = '' // means the fact should be omitted
                }
            }
        } else {
            list = []
        }

        wordMappings.addAll(list)

        RepeatStatus.FINISHED
    }
}
