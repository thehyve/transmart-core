package org.transmartproject.batch.tag

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import javax.annotation.Resource

/**
 * Tasklet for fetching tag type options.
 */
@Slf4j
@JobScope
@Component
class FetchTagTypeOptionsTasklet implements Tasklet {

    @Autowired
    TagTypeService tagTypeService

    @Resource
    TagTypeOptionsMap tagTypeOptionsMap

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        def tagTypeOptions = tagTypeService.fetchAllTagTypeOptions()
        tagTypeOptionsMap.setOptions tagTypeOptions
        int tagTypeCount = tagTypeOptions.keySet().size()
        log.info "Read options for ${tagTypeCount} tag types."
        tagTypeCount.times { contribution.incrementReadCount() }
        RepeatStatus.FINISHED
    }

}
