package org.transmartproject.batch.tag

import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import javax.annotation.Resource

/**
 * Database writer of tag types
 */
@Slf4j
@Component
@JobScope
class TagTypeWriter implements ItemWriter<TagType> {

    @Autowired
    TagTypeService tagTypeService

    @Resource TagTypesJobMetadata tagTypesJobMetadata

    @Override
    void write(List<? extends TagType> items) throws Exception {
        def deleteItems = []
        def updateItems = []
        def insertItems = []
        items.each { item ->
            log.debug "Processing tag type '${item.title}'..."
            def tagType = tagTypesJobMetadata.tagTypes[item.title]
            if (tagType == null) {
                insertItems.add(item)
                log.debug "Tag type not found. Will insert '${item.title}'."
            } else {
                if (item != (tagType)) {
                    log.debug "Tag type with name '${item.title}' already exists. Will delete and insert."
                    updateItems.add(new TagTypeUpdate(oldType: tagType, newType: item))
                } else {
                    log.debug "Equal tag type found. Skipping '${item.title}'."
                }
            }
        }

        tagTypeService.updateTagTypes(updateItems)

        tagTypeService.deleteTagTypes(deleteItems)

        tagTypeService.insert(insertItems)

        tagTypesJobMetadata.writtenTagTypes.addAll(items*.title)
    }

}
