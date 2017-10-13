package org.transmartproject.batch.tag

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.stereotype.Component

/**
 * Holds list of currently loaded tag types and set of already processed input tag types.
 */
@Component
@JobScope
@Slf4j
@CompileStatic
class TagTypesJobMetadata {

    Map<String, TagType> tagTypes

    Set<String> writtenTagTypes = []

}
