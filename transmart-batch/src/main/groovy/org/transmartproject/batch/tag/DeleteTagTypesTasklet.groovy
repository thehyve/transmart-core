package org.transmartproject.batch.tag

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired

import javax.annotation.Resource

/**
 * Deletes tag types that were not in the loaded file.
 */
@Slf4j
class DeleteTagTypesTasklet implements Tasklet {

    @Autowired
    TagTypeService tagTypeService

    @Resource
    TagTypesJobMetadata tagTypesJobMetadata

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        def deleteTagTypes = tagTypesJobMetadata.tagTypes.keySet() - tagTypesJobMetadata.writtenTagTypes

        log.debug "Checking if tag types can be safely deleted: ${deleteTagTypes}"
        def tagTypeOptionsWithReferences = tagTypeService.fetchAllTagTypeOptionsWithReferences()
        // check if tag types can be safely deleted.
        deleteTagTypes.each { String title ->
            def referencedOptions = (tagTypeOptionsWithReferences[title] ?: [:]).keySet()
            if (!referencedOptions.empty) {
                throw new InvalidTagTypeOptionsDeleteException(
                        "Cannot delete tag type '${title}', " +
                                "because of existing references."
                )
            }
        }

        log.debug "Deleting tag types: ${deleteTagTypes}"
        def deleteItems = deleteTagTypes.collect { tagType ->
            tagTypesJobMetadata.tagTypes[tagType]
        }
        int tagTypesCount = tagTypeService.deleteTagTypes(deleteItems)
        log.info "${tagTypesCount} tag types deleted."
        contribution.incrementWriteCount(tagTypesCount)
        RepeatStatus.FINISHED
    }

}
