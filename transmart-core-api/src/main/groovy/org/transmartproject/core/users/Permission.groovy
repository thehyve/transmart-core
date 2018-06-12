package org.transmartproject.core.users

/**
 * Describes a level of access that can be performed on a certain resource.
 *
 * Enum values declaration order specifies {@link Enum#compareTo(AccessLevel acclvl)} how high is the access level.
 * AGGREGATE_WITH_THRESHOLD < VIEW < EXPORT < OWN
 */
enum AccessLevel {

    /**
     * Only access to aggregates, with resolution limited by the configured
     * threshold. {@see Config}.
     */
    AGGREGATE_WITH_THRESHOLD,
    /**
     * Access to all aggregates, and all data.
     */
            VIEW,
    /**
     * Access to aggregates, and all data with possibility to export.
     *
     * LEGACY: Old transmart code distinguishes VIEW and EXPORT levels.
     * VIEW being a access level that does not allow explicit data exports.
     * Supporting this difference in the new code is highly not recommended.
     */
            @Deprecated
            EXPORT,
    /**
     * The highest access level.
     */
            OWN
}
