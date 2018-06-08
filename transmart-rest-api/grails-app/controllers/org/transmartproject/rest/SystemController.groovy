package org.transmartproject.rest

import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.config.SystemResource
import org.transmartproject.core.exceptions.AccessDeniedException
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
     * <code>/${apiVersion}/system/after_data_loading_update</code>
     *
     * This endpoint should be called after loading, deleting or updating
     * data in the database.
     * Only available for administrators.
     */
    def afterDataLoadingUpdate() {
        checkForUnsupportedParams(params, [])
        if (!authContext.user.admin) {
            throw new AccessDeniedException('Only allowed for administrators.')
        }
        systemResource.updateAfterDataLoading(authContext.user)
        response.status = 200
    }
}
