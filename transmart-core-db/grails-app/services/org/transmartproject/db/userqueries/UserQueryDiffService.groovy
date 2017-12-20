package org.transmartproject.db.userqueries

import grails.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.userquery.UserQueryDiff
import org.transmartproject.core.userquery.UserQueryDiffResource
import org.transmartproject.core.users.User
import org.transmartproject.db.user.User as DbUser
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.accesscontrol.AccessControlChecks

@Transactional
class UserQueryDiffService implements UserQueryDiffResource {

    @Autowired
    UsersResource usersResource

    @Autowired
    AccessControlChecks accessControlChecks

    @Override
    void scan(User currentUser) {
        org.transmartproject.db.user.User user = (DbUser) usersResource.getUserFromUsername(currentUser.username)
        if (!user.admin) {
            throw new AccessDeniedException('Only allowed for administrators.')
        }
        // todo implement the scan
    }

    @Override
    List<UserQueryDiff> getByQueryId(Long queryId, int firstResult, int numResults) {

    }

    @Override
    List<UserQueryDiff> getByFrequency(String frequency, String username, int firstResult, int numResults) {

    }
}
