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

package org.transmartproject.db.accesscontrol

import groovy.transform.EqualsAndHashCode
import org.transmartproject.db.user.User

@EqualsAndHashCode(includes = 'securedObjectAccess,user,securedObject,accessLevel')
class SecuredObjectAccessView implements Serializable {

    /* It's a view!
     *
     * It has the same columns as SecuredObjectAccess, except that instead of
     * a principal id, it has a user id. It is a union of:
     *
     * - The entries of SecureObjectAccess that refer to users directly.
     * - The entries of SecureObjectAccess that refer to groups with the
     *   group replaced with the users that belong to that group (so the
     *   cardinality will be higher)
     * - The entries of SecureObjectAccess where the group is the
     *   EVERYONE_GROUP. In this case, the principal id column (here the user id
     *   column) will be replaced with NULL.
     */

    SecuredObjectAccess  securedObjectAccess
    User                 user
    SecuredObject        securedObject
    AccessLevel          accessLevel



    static mapping = {
        table schema: 'searchapp', name: 'search_auth_user_sec_access_v'

        id composite: ['securedObjectAccess', 'user', 'securedObject', 'accessLevel']

        securedObjectAccess column: 'search_auth_user_sec_access_id'
        user                column: 'search_auth_user_id'
        securedObject       column: 'search_secure_object_id'
        accessLevel         column: 'search_sec_access_level_id'

        cache usage: 'read-only'

        version false
    }
}
