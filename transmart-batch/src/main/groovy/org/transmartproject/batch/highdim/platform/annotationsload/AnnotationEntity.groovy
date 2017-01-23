package org.transmartproject.batch.highdim.platform.annotationsload

import groovy.transform.Immutable

/**
 * Simple pair associating a logical name for a high-dimensional platform entity
 * such as probes to their internal id (such as the probeset_id).
 */
@Immutable
class AnnotationEntity {
    String name
    Long internalId
}
