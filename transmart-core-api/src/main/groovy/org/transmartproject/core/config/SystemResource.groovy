package org.transmartproject.core.config

import org.transmartproject.core.exceptions.ServiceNotAvailableException

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

    /**
     * Clears the tree node cache and the counts caches, and
     * rebuild the tree node cache for every user.
     *
     * This function should be called after loading, removing or updating
     * tree nodes or observations in the database.
     *
     * Asynchronous call. The call returns when rebuilding has started.
     *
     * @throws org.transmartproject.core.exceptions.ServiceNotAvailableException iff an update operation is already in progress.
     *
     */
    UpdateStatus rebuildCache() throws ServiceNotAvailableException

}
