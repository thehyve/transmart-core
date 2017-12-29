package org.transmartproject.db.userqueries

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.TransmartSpecification

@Rollback
@Integration
class UserQueryDiffServiceSpec extends TransmartSpecification {

    @Autowired
    UserQueryDiffService userQueryDiffService

    @Autowired
    SessionFactory sessionFactory

    UserQueryTestData userQueryTestData

    void setupData() {
        userQueryTestData = UserQueryTestData.createDefault()
        userQueryTestData.saveAll()
    }

    void "test fetching queryDiffs for a query"() {
        setupData()

        when:
        def user = userQueryTestData.user
        def result = userQueryDiffService.getAllEntriesByQueryId(userQueryTestData.queries[0].id, user, 0, 20)

        then:
        result != null

        // check queryDiffEntries
        result.size() == 4
        result.containsAll(userQueryTestData.queryDiffEntries[0], userQueryTestData.queryDiffEntries[1],
                userQueryTestData.queryDiffEntries[2], userQueryTestData.queryDiffEntries[3])

        // check queryDiffs
        (result.queryDiff as Set).size() == 2
        result.queryDiff.containsAll(userQueryTestData.queryDiffs[0], userQueryTestData.queryDiffs[1])

        // check query
        (result.queryDiff.query as Set).size() == 1
        result.queryDiff.query.contains(userQueryTestData.queries[0])

    }
}



