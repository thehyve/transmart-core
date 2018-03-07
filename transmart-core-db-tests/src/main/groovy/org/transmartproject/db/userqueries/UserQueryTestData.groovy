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

package org.transmartproject.db.userqueries

import org.transmartproject.db.TestData
import org.transmartproject.db.user.RoleCoreDb
import org.transmartproject.db.user.User

import static org.transmartproject.db.TestDataHelper.save
import org.transmartproject.db.querytool.Query
import org.transmartproject.db.querytool.QuerySet
import org.transmartproject.db.querytool.QuerySetInstance

class UserQueryTestData {

    List<Query> queries
    List<QuerySet> querySets
    List<QuerySetInstance> querySetInstances

    static TestData testData

    static User user
    static User adminUser
    static RoleCoreDb adminRole

    static UserQueryTestData createDefault() {
        testData = TestData.createDefault()
        user = createUser()
        adminUser = createAdminUser()
        def result = new UserQueryTestData()
        result.queries = createTestQuery()
        result.querySets = createQuerySetsForQueries(result.queries)
        result.querySetInstances = createQuerySetInstances(result.querySets)
        result
    }

    static List<QuerySetInstance> createQuerySetInstances(List<QuerySet> querySets) {
        List<QuerySetInstance> entries = []

        for (int i = 0; i < querySets.size(); i++) {
            entries.add(
                    new QuerySetInstance(
                            querySet: querySets[i],
                            objectId: testData.i2b2Data.patients[0].id
                    )
            )
            entries.add(
                    new QuerySetInstance(
                            querySet: querySets[i],
                            objectId: testData.i2b2Data.patients[1].id
                    )
            )
        }
        entries
    }

    static List<QuerySet> createQuerySetsForQueries(List<Query> queries) {
        List<QuerySet> diffs = []

        for (int i = 0; i < queries.size(); i++) {
            diffs.add(
                    new QuerySet(
                            query: queries[i],
                            setSize: 2,
                            setType: 'PATIENT'
                    )
            )
        }
        diffs

    }

    static List<Query> createTestQuery() {
        List<Query> queries = []

        queries.add(
                new Query(
                        name: 'test query 1',
                        patientsQuery: '{type: "true"}',
                        observationsQuery: '{type: "true"}',
                        bookmarked: true,
                        subscribed: true,
                        subscriptionFreq: 'DAILY',
                        username: user.username,
                        apiVersion: 'v2_test'
                )
        )
        queries.add(
                new Query(
                        name: 'test query 2',
                        patientsQuery: '{type: "true"}',
                        observationsQuery: null,
                        bookmarked: false,
                        subscribed: true,
                        subscriptionFreq: 'WEEKLY',
                        username: user.username,
                        apiVersion: 'v2_test'
                )
        )
        queries
    }

    static User createUser() {
        def user = new User(
                username: 'fake-user',
                uniqueId: 'fake-user',
                enabled: true)

        user.id = 100
        return user
    }

    static User createAdminUser() {
        def adminUser = new User(
                username: 'admin',
                uniqueId: 'admin',
                enabled: true)
        adminUser.id = 101

        adminRole = new RoleCoreDb(
                authority: 'ROLE_ADMIN',
                description: 'admin user'
        )
        adminRole.id = 101
        adminUser.addToRoles(adminRole)
        return adminUser
    }

    def saveAll() {
        testData.saveAll()
        user.save()
        adminRole.save()
        adminUser.save()
        save queries
        save querySets
        save querySetInstances
    }
}
