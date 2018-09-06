package org.transmartproject.core.users

/**
 * Describes a level of read access to the patient data.
 *
 * Enum values declaration order specifies {@link Enum#compareTo(PatientDataAccessLevel)} how high is the access level.
 * COUNTS_WITH_THRESHOLD < COUNTS < SUMMARY < MEASUREMENTS
 */
enum PatientDataAccessLevel {

    /**
     * Only access to patient counts, with resolution limited by the configured
     * threshold.
     */
    COUNTS_WITH_THRESHOLD,

    /**
     * Only access to patient counts
     */
    COUNTS,

    /**
     * Access to summary statistic. e.g. histogram
     *
     * Used to support VIEW vs EXPORT difference in transmartApp
     * It is used as VIEW replacement.
     */
    SUMMARY,

    /**
     * Read access to the observational measurements.
     *
     * Used to support VIEW vs EXPORT difference in transmartApp
     * It is used as EXPORT replacement.
     */
    MEASUREMENTS

    static PatientDataAccessLevel getMinimalAccessLevel() {
        Collections.min(Arrays.asList(values()))
    }
}
