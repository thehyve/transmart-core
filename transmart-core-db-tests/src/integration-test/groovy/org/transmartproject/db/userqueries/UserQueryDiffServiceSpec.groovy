package org.transmartproject.db.userqueries

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.db.TransmartSpecification
import org.transmartproject.db.querytool.QtQueryInstance
import org.transmartproject.db.querytool.QtQueryMaster
import org.transmartproject.db.querytool.QtQueryResultInstance

@Integration
@Rollback
class UserQueryDiffServiceSpec extends TransmartSpecification {


    UserQueryDiffService userQueryDiffService

    SessionFactory sessionFactory

    UserQueryTestData userQueryTestData

    void setupData() {
        userQueryTestData = UserQueryTestData.createDefault()
        userQueryTestData.saveAll()
        sessionFactory.currentSession.flush()
    }

    void "test fetching queryDiffs for a query"() {
        setupData()

        when:
        def user = userQueryTestData.user
        def result = userQueryDiffService.getAllEntriesByQueryId(userQueryTestData.queries[0].id, user, 0, 20)

        then:
        result != null

        // check queryDiffEntries
        result.size() == 2
        result.containsAll(userQueryTestData.queryDiffEntries[0], userQueryTestData.queryDiffEntries[1])

        // check queryDiffs
        (result.queryDiff as Set).size() == 1
        result.queryDiff.containsAll(userQueryTestData.queryDiffs[0])

        // check query
        (result.queryDiff.query as Set).size() == 1
        result.queryDiff.query.contains(userQueryTestData.queries[0])

    }

    void "test scanning for patientSets changes"() {
        setupData()

        when:
        // user is not admin
        def user = userQueryTestData.user
        def result = userQueryDiffService.scan(user)

        then:
        AccessDeniedException ex = thrown()
        ex.message == 'Only allowed for administrators.'

        when:
        // user is not admin
        user = userQueryTestData.adminUser
        result = userQueryDiffService.scan(user)

        then:
        result != null
        // number of updated patient sets
        result == 2
    }

}



