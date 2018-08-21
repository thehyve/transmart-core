package org.transmartproject.core.config


interface CountsThresholdResource {

    /**
     * Get threshold value, below which counts are not available for users
     * with `COUNTS_WITH_THRESHOLD` access permission.
     *
     * @return threshold value
     */
    long getPatientCountThreshold()

}