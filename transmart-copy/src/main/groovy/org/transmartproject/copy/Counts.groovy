package org.transmartproject.copy

import groovy.transform.CompileStatic

/**
 * Wrapper for inserted, updated and existing record counts.
 */
@CompileStatic
class Counts {
    long insertCount = 0L
    long updatedCount = 0L
    long existingCount = 0L
}
