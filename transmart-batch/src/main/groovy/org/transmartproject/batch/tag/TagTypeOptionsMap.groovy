package org.transmartproject.batch.tag

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.stereotype.Component

/**
 * Structure to hold all the tag types and associated options.
 */
@Component
@JobScope
@Slf4j
@CompileStatic
class TagTypeOptionsMap {

    private final Map<String, Map<String, Integer>> tagTypeToOptionsMap = [:]

    void setOptions(Map<String, Map<String, Integer>> tagTypeToOptionsMap) {
        this.tagTypeToOptionsMap.clear()
        this.tagTypeToOptionsMap.putAll(tagTypeToOptionsMap)
    }

    /**
     * Return the map from tag description to tag option id for all options
     * associated with the tag type.
     *
     * @param tagType
     * @return a map from tag description to tag option id.
     */
    Map<String, Integer> getOptionsForTagType(String tagType) {
        tagTypeToOptionsMap[tagType]
    }

}
