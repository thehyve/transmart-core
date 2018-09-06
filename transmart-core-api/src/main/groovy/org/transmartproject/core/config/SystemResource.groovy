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
     * Checks if a cache rebuild task is active.
     * Only available for administrators.
     *
     * @return true iff a cache rebuild task is active.
     */
    boolean isRebuildActive()

    /**
     * Clears the tree node cache and the counts caches, and
     * rebuild the tree node cache for every user.
     *
     * This function should be called after loading, removing or updating
     * tree nodes or observations in the database.
     *
     * Asynchronous call. The call returns when rebuilding has started.
     *
     * @throws org.transmartproject.core.exceptions.ServiceNotAvailableException iff a rebuild operation is already in progress.
     *
     */
    void rebuildCache() throws ServiceNotAvailableException

}
