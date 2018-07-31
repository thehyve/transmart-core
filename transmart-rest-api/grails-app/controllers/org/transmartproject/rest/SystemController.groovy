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
     * updates data for subscribed user queries:
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

}
