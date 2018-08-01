package org.transmartproject.core.config

interface SystemResource {

    RuntimeConfig getRuntimeConfig()

    RuntimeConfig updateRuntimeConfig(RuntimeConfig config)

    /**
     * Clears the tree node cache. This function should be called after loading, removing or updating
     * tree nodes in the database.
     */
    void clearCaches()

    /**
     * Clears the caches, patient sets, refreshes a materialized view with study_concept bitset
     * and scans for the changes for subscribed user queries.
     * @param currentUser
     */
    UpdateStatus updateAfterDataLoading()

    /**
     * Returns the status of the current update task.
     * @param currentUser
     */
    UpdateStatus getUpdateStatus()

}
