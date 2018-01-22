package org.transmartproject.db.userqueries

import grails.transaction.Transactional
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.userquery.UserQuery
import org.transmartproject.core.userquery.UserQueryResource
import org.transmartproject.core.users.User
import org.transmartproject.db.querytool.Query

@Transactional
class UserQueryService implements UserQueryResource {

    @Override
    List<UserQuery> list(User currentUser) {
        Query.findAllByUsernameAndDeleted(currentUser.username, false)
    }

    @Override
    List<UserQuery> listSubscribed() {
        Query.findAllByDeletedAndSubscribed(false, true)
    }

    @Override
    UserQuery get(Long id, User currentUser) {
        Query query = Query.findByIdAndDeleted(id, false)
        if (!query) {
            throw new NoSuchResourceException("Query with id ${id} has not found.")
        }
        if (currentUser.username != query.username) {
            throw new AccessDeniedException("Query does not belong to the current user.")
        }
        query
    }

    @Override
    UserQuery create(User currentUser) {
        new Query(username: currentUser.username)
    }

    @Override
    void save(UserQuery query, User currentUser) {
        assert query instanceof Query
        if (currentUser.username != query.username) {
            throw new AccessDeniedException("Query does not belong to the current user.")
        }
        if (!query.validate()) {
            def message = query.errors.allErrors*.defaultMessage.join('.')
            throw new InvalidArgumentsException(message)
        }
        query.updateDate = new Date()
        query.save(flush: true, failOnError: true)
    }

    @Override
    void delete(Long id, User currentUser) {
        def query = get(id, currentUser)
        assert query instanceof Query
        query.deleted = true
        save(query, currentUser)
    }
}
