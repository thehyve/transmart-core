package org.transmartproject.batch.highdim.platform.annotationsload

import com.google.common.collect.Maps

/**
 * Collects the {@link AnnotationEntity}s to expose a map between
 * logical entity names and internal database ids.
 */
class AnnotationEntityMap {

    private final Map<String, Long> map = Maps.newTreeMap()

    void leftShift(AnnotationEntity entity) {
        map[entity.name] = entity.internalId
    }

    Long getAt(String logicalName) {
        map[logicalName]
    }

    SortedSet<String> getAnnotationNames() {
        map.keySet()
    }
}
