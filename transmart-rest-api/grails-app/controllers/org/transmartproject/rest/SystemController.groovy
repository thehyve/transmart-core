package org.transmartproject.rest

import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.config.SystemResource
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.rest.user.AuthContext

import static org.transmartproject.rest.misc.RequestUtils.checkForUnsupportedParams

class SystemController {

    @Autowired
    SystemResource systemResource

    @Autowired
    AuthContext authContext

    /**
     * Clears tree node, counts caches, patient sets and bitsets,
     * updates data for subscribed user queries,
     * rebuilds the cache:
     * <code>GET /v2/admin/system/after_data_loading_update</code>
     * Returns an {@link org.transmartproject.core.config.UpdateStatus} object.
     *
     * This endpoint should be called after loading, deleting or updating
     * data in the database.
     *
     * Only available for administrators.
     */
    def afterDataLoadingUpdate() {
        checkForUnsupportedParams(params, [])
        if (!authContext.user.admin) {
            throw new AccessDeniedException('Only allowed for administrators.')
        }
        respond systemResource.updateAfterDataLoading()
    }

    /**
     * Checks if an update task is running:
     * <code>GET /v2/admin/system/update_status</code>
     * Returns an {@link org.transmartproject.core.config.UpdateStatus} object.
     *
     * Only available for administrators.
     */
    def updateStatus() {
        checkForUnsupportedParams(params, [])
        if (!authContext.user.admin) {
            throw new AccessDeniedException('Only allowed for administrators.')
        }
        def updateStatus = systemResource.updateStatus
        if (updateStatus) {
            respond updateStatus
        } else {
            throw new NoSuchResourceException("No update task found.")
        }
    }

    /**
     * Clears tree node and counts caches and rebuilds the cache:
     * <code>GET /v2/admin/system/rebuild_cache</code>
     *
     * This endpoint should be called after loading, deleting or updating
     * tree nodes in the database.
     * Only available for administrators.
     *
     * Asynchronous call. The call returns when rebuilding has started.
     * Code 503 is returned if a rebuild operation is already in progress.
     *
     */
    def rebuildCache() {
        checkForUnsupportedParams(params, [])
        if (!authContext.user.admin) {
            throw new AccessDeniedException('Only allowed for administrators.')
        }
        systemResource.rebuildCache()
        response.status = 200
    }

    /**
     * Checks if a cache rebuild task is running:
     * <code>GET /v2/admin/system/rebuild_status</code>
     * Returns an object with a field <code>status</code> with value 'running'
     * or 'stopped'.
     * Example: <code>{"status": "running"}</code>.
     *
     * Only available for administrators.
     *
     * @return an object with a field <code>status</code> with value <code>running</code>
     * or <code>stopped</code>.
     *
     */
    def rebuildStatus() {
        checkForUnsupportedParams(params, [])
        if (!authContext.user.admin) {
            throw new AccessDeniedException('Only allowed for administrators.')
        }
        respond status: systemResource.isRebuildActive() ? 'running' : 'stopped'
    }

    /**
     * Clears tree node and counts caches:
     * <code>GET /v2/admin/system/tree_nodes/clear_cache</code>
     *
     * This endpoint should be called after loading, deleting or updating
     * tree nodes in the database.
     * Only available for administrators.
     *
     * @deprecated in favour of {@link SystemController#afterDataLoadingUpdate()}
     */
    @Deprecated
    def clearCache() {
        checkForUnsupportedParams(params, [])
        if (!authContext.user.admin) {
            throw new AccessDeniedException('Only allowed for administrators.')
        }
        systemResource.clearCaches()
        response.status = 200
    }

}
