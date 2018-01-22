package org.transmartproject.db.userqueries

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.hibernate.SessionFactory
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.db.TransmartSpecification

@Integration
@Rollback
class UserQuerySetServiceSpec extends TransmartSpecification {


    UserQuerySetService userQueryDiffService

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
        def result = userQueryDiffService.getDiffEntriesByQueryId(userQueryTestData.queries[0].id, user, 0, 20)

        then:
        result != null

        // check querySetInstances
        result.size() == 2
        result.containsAll(userQueryTestData.querySetInstances[0], userQueryTestData.querySetInstances[1])

        // check querySets
        (result.queryDiff as Set).size() == 1
        result.queryDiff.containsAll(userQueryTestData.querySets[0])

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



