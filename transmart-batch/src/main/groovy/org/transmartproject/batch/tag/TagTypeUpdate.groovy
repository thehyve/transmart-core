package org.transmartproject.batch.tag

/**
 * Represents an update to a Tag Type.
 */
class TagTypeUpdate {

    TagType oldType
    TagType newType

    Set<String> addedOptions() {
        (newType.values as Set) - (oldType.values as Set)
    }

    Set<String> deletedOptions() {
        (oldType.values as Set) - (newType.values as Set)
    }
}
