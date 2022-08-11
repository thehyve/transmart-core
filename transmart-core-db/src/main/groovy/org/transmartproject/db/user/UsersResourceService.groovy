/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.user

import grails.gorm.transactions.Transactional
import org.hibernate.Query
import org.springframework.stereotype.Component
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.users.UsersResource

import java.security.Principal

@Component
class UsersResourceService implements UsersResource {

    @Transactional(readOnly = true)
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

        User user = User.withSession { session ->
            Query query = session.createQuery(
                    'FROM User u LEFT JOIN FETCH u.roles WHERE u.username = ?0')
            query.setParameter 0, username
            def users = query.list()

            users[0]
        }

        if (!user) {
            throw new NoSuchResourceException("No user with username " +
                    "$username was found")
        }

        user
    }

    @Transactional(readOnly = true)
    @Override
    List<org.transmartproject.core.users.User> getUsers() {
        User.withSession { session ->
            session.createQuery('FROM User u LEFT JOIN FETCH u.roles').list()
        }.unique()
    }

    @Transactional(readOnly = true)
    @Override
    List<org.transmartproject.core.users.User> getUsersWithEmailSpecified() {
        User.withSession { session ->
            session.createQuery('FROM User u WHERE u.email IS NOT NULL').list()
        } as List<org.transmartproject.core.users.User>
    }

    @Override
    org.transmartproject.core.users.User getUserFromPrincipal(Principal principal) {
        return getUserFromUsername(principal.name)
    }
}
