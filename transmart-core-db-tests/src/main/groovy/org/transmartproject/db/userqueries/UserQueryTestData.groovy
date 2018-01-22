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

import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.querytool.QtQueryMaster
import org.transmartproject.db.user.RoleCoreDb
import org.transmartproject.db.user.User

import static org.transmartproject.db.TestDataHelper.save
import org.transmartproject.db.querytool.Query
import org.transmartproject.db.querytool.QuerySet
import org.transmartproject.db.querytool.QuerySetInstance
import static org.transmartproject.db.querytool.QueryResultData.createQueryResult
import static org.transmartproject.db.querytool.QueryResultData.getQueryResultFromMaster

class UserQueryTestData {

    List<Query> queries
    List<QuerySet> querySets
    List<QuerySetInstance> querySetInstances

    static User user
    static User adminUser
    static RoleCoreDb adminRole
    static List<PatientDimension> patients
    static QueryResult queryResult
    static QtQueryMaster patientsQueryMaster

    private static int number = 2

    static UserQueryTestData createDefault() {
        user = createUser()
        adminUser = createAdminUser()
        createAndSavePatientSet()
        queryResult = getQueryResultFromMaster patientsQueryMaster
        def result = new UserQueryTestData()
        result.queries = createTestQueries(1)
        result.querySets = createQueryDiffsForQueries(number, result.queries)
        result.querySetInstances = createQueryDiffEntriesForQueryDiffs(number, result.querySets)
        result
    }

    private static void createAndSavePatientSet() {
        patients = createTestPatients(3, -100, 'STUDY_ID_1')
        patientsQueryMaster = createQueryResult patients
        save patients
        patientsQueryMaster.save()
    }

    static List<QuerySetInstance> createQueryDiffEntriesForQueryDiffs(int number, List<QuerySet> querySets) {
        List<QuerySetInstance> entries = []

        for (int i = 0; i < number; i++) {
            entries.add(
                    new QuerySetInstance(
                            querySet: querySets[i],
                            objectId: patients[0].id
                    )
            )
            entries.add(
                    new QuerySetInstance(
                            querySet: querySets[i],
                            objectId: -2
                    )
            )
        }
        entries
    }

    static List<QuerySet> createQueryDiffsForQueries(int number, List<Query> queries) {
        List<QuerySet> diffs = []

        for (int i = 0; i < number; i++) {
            diffs.add(
                    new QuerySet(
                            query: queries[i],
                            setId: queryResult.id,
                            setType: 'PATIENT'
                    )
            )
        }
        diffs

    }

    static List<Query> createTestQueries(int number) {
        List<Query> queries = []

        for (int i = 0; i < number; i++) {
            queries.add(
                    new Query(
                            name             : 'test query 1',
                            patientsQuery    : '{type: "true"}',
                            observationsQuery: '{type: "true"}',
                            bookmarked       : true,
                            subscribed       : true,
                            subscriptionFreq : 'DAILY',
                            username         : user.username,
                            apiVersion       : 'v2_test'
                    )
            )
            queries.add(
                    new Query(
                            name             : 'test query 2',
                            patientsQuery    : '{type: "true"}',
                            observationsQuery: null,
                            bookmarked       : false,
                            subscribed       : true,
                            subscriptionFreq : 'WEEKLY',
                            username         : user.username,
                            apiVersion       : 'v2_test'
                    )
            )
        }
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

    static List<PatientDimension> createTestPatients(int n, long baseId, String trialName = 'SAMP_TRIAL') {
        (1..n).collect { int i ->
            def p = new PatientDimension()
            p.id = baseId - i
            p.sourcesystemCd = "$trialName:SUBJ_ID_$i"
            p
        }
    }

    def saveAll() {
        user.save()
        adminRole.save()
        adminUser.save()
        save queries
        save querySets
        save querySetInstances
    }
}
