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

import groovy.util.logging.Slf4j
import org.hibernate.FetchMode
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.db.accesscontrol.AccessLevelCoreDb
import org.transmartproject.db.accesscontrol.SecuredObject
import org.transmartproject.db.accesscontrol.SecuredObjectAccessView

import static org.hibernate.sql.JoinType.INNER_JOIN

@Slf4j
class User extends PrincipalCoreDb implements org.transmartproject.core.users.User {

    String email
    Boolean emailShow
    String hash
    String realName
    String username

    /* not mapped (only on thehyve/master) */
    //String federatedId

    static hasMany = [
            roles : RoleCoreDb,
            groups: Group
    ]

    static transients = ['admin']

    static mapping = {
        //table   schema: 'searchapp', name: 'search_auth_user'
        // ^^ Bug! doesn't work
        table name: 'searchapp.search_auth_user'

        hash column: 'passwd'

        // no way to fetch the roles' properties themselves :(
        // http://stackoverflow.com/questions/4208728
        roles joinTable: [//name:   'search_role_auth_user',
                          name  : 'searchapp.search_role_auth_user',
                          key   : 'authorities_id',
                          column: 'people_id'], // insane column naming!
                fetch: FetchMode.JOIN

        groups joinTable: [//name:   'search_auth_group_member',
                           name  : 'searchapp.search_auth_group_member',
                           key   : 'auth_user_id',
                           column: 'auth_group_id']

        discriminator name: 'USER', column: 'unique_id'

        cache usage: 'read-only', include: 'non-lazy' /* don't cache groups */

        realName column: 'user_real_name'

        version false
    }

    static constraints = {
        email nullable: true, maxSize: 255
        emailShow nullable: true
        hash nullable: true, maxSize: 255
        realName nullable: true, maxSize: 255
        username nullable: true, maxSize: 255
        //federatedId nullable: true, unique: true
    }

    @Override
    boolean isAdmin() {
        roles.find { it.authority == RoleCoreDb.ROLE_ADMIN_AUTHORITY }
    }

    @Override
    Map<String, PatientDataAccessLevel> getStudyToPatientDataAccessLevel() {
        List<Object[]> securedObjectWithaccessLevelPairs = SecuredObjectAccessView.createCriteria().list {
            projections {
                property('securedObject')
                property('accessLevel')
            }
            createAlias('securedObject', 'so', INNER_JOIN)
            or {
                eq('user', this)
                isNull('user')
            }
            eq('so.dataType', SecuredObject.STUDY_DATA_TYPE)
        }
        Map<String, PatientDataAccessLevel> result = [:]
        for (Object[] securedObjectWithaccessLevelPair : securedObjectWithaccessLevelPairs) {
            def (SecuredObject securedObject, AccessLevelCoreDb accessLevelDb) = securedObjectWithaccessLevelPair
            String studyToken = securedObject.bioDataUniqueId
            if (result.containsKey(studyToken)) {
                PatientDataAccessLevel memorisedAccLvl = result.get(studyToken)
                if (accessLevelDb.accessLevel > memorisedAccLvl) {
                    log.debug("Use ${accessLevelDb.accessLevel} access level instead of ${memorisedAccLvl} for ${username} user on ${studyToken} study token.")
                    result.put(studyToken, accessLevelDb.accessLevel)
                } else {
                    log.debug("Keep ${memorisedAccLvl} access level and ignore ${accessLevelDb.accessLevel} for ${username} user on ${studyToken} study token.")
                }
            } else {
                result.put(studyToken, accessLevelDb.accessLevel)
                log.debug("${username} user has ${accessLevelDb.accessLevel} access level on ${studyToken} study token.")
            }
        }
        result
    }
}
