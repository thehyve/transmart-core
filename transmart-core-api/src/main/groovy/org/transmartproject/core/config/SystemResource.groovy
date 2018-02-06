package org.transmartproject.core.config

interface SystemResource {

    RuntimeConfig getRuntimeConfig()

    RuntimeConfig updateRuntimeConfig(RuntimeConfig config)

    /**
     * Clears the tree node cache. This function should be called after loading, removing or updating
     * tree nodes in the database.
     */
    void clearCaches()

}
