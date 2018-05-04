package org.transmartproject.rest

import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.config.SystemResource
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.user.User
import org.transmartproject.rest.misc.CurrentUser

import static org.transmartproject.rest.misc.RequestUtils.checkForUnsupportedParams

class SystemController {

    @Autowired
    SystemResource systemResource

    @Autowired
    CurrentUser currentUser

    @Autowired
    UsersResource usersResource

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
        User dbUser = (User) usersResource.getUserFromUsername(currentUser.username)
        if (!dbUser.admin) {
            throw new AccessDeniedException('Only allowed for administrators.')
        }
        systemResource.updateAfterDataLoading(dbUser)
        response.status = 200
    }
}
