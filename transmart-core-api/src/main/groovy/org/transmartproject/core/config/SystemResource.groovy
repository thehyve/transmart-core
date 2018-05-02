package org.transmartproject.core.config

import org.transmartproject.core.users.User

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
    void updateAfterDataLoading(User currentUser)

}
