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

import org.transmartproject.core.users.AccessLevel

import static java.lang.Enum.valueOf

class AccessLevelCoreDb {

    String name
    Long value

    static hasMany = [searchAuthSecObjectAccesses: SecuredObjectAccess]

    static mapping = {
        table schema: 'searchapp', name: 'search_sec_access_level'

        id column: 'search_sec_access_level_id', generator: 'assigned'
        name column: 'access_level_name'
        value column: 'access_level_value'

        version false
    }

    static constraints = {
        name nullable: false, maxSize: 200
        value nullable: true
    }

    @Override
    String toString() {
        com.google.common.base.Objects.toStringHelper(this)
                .add("name", name)
                .add("value", value).toString()
    }

    AccessLevel getAccessLevel() {
        valueOf(AccessLevel, name)
    }
}
