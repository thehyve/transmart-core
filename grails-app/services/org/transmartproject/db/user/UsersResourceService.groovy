package org.transmartproject.db.user

import org.hibernate.Query
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.users.UsersResource

class UsersResourceService implements UsersResource {

    @Override
    org.transmartproject.core.users.User getUserFromUsername(String username)
            throws NoSuchResourceException {

        /* prefetch the roles so that the object can be used when detached.
         * This saves us from having to reattach (or more likely refetch, since
         * we pass the user to quartz/job threads that have their own session
         * and an object can't be associated with two sessions at the same time)
         * the entity when it's used in contexts where the original session is
         * unavailable, like other threads. Having to do that is not only
         * slightly annoying, it also leaks the User abstraction by making it
         * clear that the User implementation is an Hibernate object.
         */

        def user = User.withSession { session ->
            if (session.respondsTo('createQuery', String)) {
                Query query = session.createQuery(
                        'FROM User u LEFT JOIN FETCH u.roles WHERE u.username = ?')
                query.setParameter 0, username
                def users = query.list()

                users[0]
            } else {
                // in case hibernate is not in use (unit tests)
                def user = User.findByUsername username
                user?.roles

                user
            }
        }

        if (!user) {
            throw new NoSuchResourceException("No user with username " +
                    "$username was found")
        }
        
        user
    }
}
