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

import org.transmartproject.db.user.User

import static org.transmartproject.db.TestDataHelper.save
import org.transmartproject.db.querytool.Query
import org.transmartproject.db.querytool.QueryDiff
import org.transmartproject.db.querytool.QueryDiffEntry


class UserQueryTestData {

    List<Query> queries
    List<QueryDiff> queryDiffs
    List<QueryDiffEntry> queryDiffEntries

    static User user = createUser()

    private static int number = 2

    static UserQueryTestData createDefault() {
        def result = new UserQueryTestData()
        result.queries = createTestQueries(number)
        result.queryDiffs = createQueryDiffsForQueries(number, result.queries)
        result.queryDiffEntries = createQueryDiffEntriesForQueryDiffs(number, result.queryDiffs)
        result
    }

    static List<QueryDiffEntry> createQueryDiffEntriesForQueryDiffs(int number, List<QueryDiff> queryDiffs) {
        List<QueryDiffEntry> entries = []

        for (int i = 0; i < number; i++) {
            entries.add(
                    new QueryDiffEntry(
                            queryDiff: queryDiffs[i],
                            objectId: -1,
                            changeFlag: 'ADDED'
                    )
            )
            entries.add(
                    new QueryDiffEntry(
                            queryDiff: queryDiffs[i],
                            objectId: -2,
                            changeFlag: 'REMOVED'
                    )
            )
        }
        entries
    }

    static List<QueryDiff> createQueryDiffsForQueries(int number, List<Query> queries) {
        List<QueryDiff> diffs = []

        for (int i = 0; i < number; i++) {
            diffs.add(
                    new QueryDiff(
                            query: queries[i],
                            setId: -1,
                            setType: 'PATIENT'
                    )
            )
            diffs.add(
                    new QueryDiff(
                            query: queries[i],
                            setId: -2,
                            setType: 'PATIENT',
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
                            patientsQuery    : '{"constraint":{"studyId":"test","type":"study_name"},' +
                                    '"type":"subselection","dimension":"patient"}',
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
                            patientsQuery    : '{"constraint":{"studyId":"test","type":"study_name"},' +
                                    '"type":"subselection","dimension":"patient"}',
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
                username: 'test_user',
                uniqueId: 'test_user',
                enabled: true)

        user.id = 100
        return user

    }

    def saveAll() {
        user.save()
        save queries
        save queryDiffs
        save queryDiffEntries
    }
}
