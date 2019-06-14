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

package org.transmartproject.db.querytool

import org.springframework.validation.Errors
import org.transmartproject.core.userquery.SubscriptionFrequency
import org.transmartproject.core.userquery.UserQuery

@Deprecated
class Query implements UserQuery {

    String name
    String username
    String patientsQuery
    String observationsQuery
    String apiVersion
    Boolean bookmarked = false
    Boolean deleted = false
    Boolean subscribed = false
    SubscriptionFrequency subscriptionFreq
    Date createDate = new Date()
    Date updateDate = new Date()
    String queryBlob

    void updateUpdateDate() {
        updateDate = new Date()
    }

    static mapping = {
        table schema: 'BIOMART_USER'
        id generator: 'sequence', params: [sequence: 'query_id_seq', schema: 'biomart_user']
        version false
    }

    static constraints = {
        name maxSize: 1000
        username maxSize: 50
        patientsQuery nullable: true
        observationsQuery nullable: true, validator: { val, obj, Errors errors ->
            if (!val && !obj.patientsQuery) {
                errors.reject(
                        'org.transmartproject.db.querytool.query.emptyQueries',
                        'patientsQuery or observationsQuery has to be not null.')
            }
        }
        apiVersion nullable: true, maxSize: 25
        bookmarked nullable: true
        subscribed nullable: true
        subscriptionFreq nullable: true
        deleted nullable: true
        createDate nullable: true
        updateDate nullable: true
        queryBlob nullable: true
    }

}
