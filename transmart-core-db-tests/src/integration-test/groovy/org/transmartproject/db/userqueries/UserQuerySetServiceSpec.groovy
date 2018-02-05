package org.transmartproject.db.userqueries

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.hibernate.SessionFactory
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.userquery.ChangeFlag
import org.transmartproject.db.TransmartSpecification
import org.transmartproject.db.querytool.QuerySet
import org.transmartproject.db.querytool.QuerySetDiff

@Integration
@Rollback
class UserQuerySetServiceSpec extends TransmartSpecification {


    UserQuerySetService userQuerySetService

    SessionFactory sessionFactory

    UserQueryTestData userQueryTestData

    void setupData() {
        userQueryTestData = UserQueryTestData.createDefault()
        userQueryTestData.saveAll()
        sessionFactory.currentSession.flush()
    }

    void "test fetching querySetInstances by a query id"() {
        setupData()

        when:
        def user = userQueryTestData.user
        def result = userQuerySetService.getSetInstancesByQueryId(userQueryTestData.queries[0].id, user, 0, 999)

        then:
        result != null

        // check querySetInstances
        result.size() == 2
        result.containsAll(userQueryTestData.querySetInstances[0], userQueryTestData.querySetInstances[1])

        // check querySets
        (result.querySet as Set).size() == 1
        result.querySet.containsAll(userQueryTestData.querySets[0])

        // check query
        (result.querySet.query as Set).size() == 1
        result.querySet.query.contains(userQueryTestData.queries[0])

    }

    void "test scanning for query set changes"() {
        setupData()

        when:
        // user is not admin
        def user = userQueryTestData.user
        def result = userQuerySetService.scan(user)

        then:
        AccessDeniedException ex = thrown()
        ex.message == 'Only allowed for administrators.'

        when:
        // user is not admin
        user = userQueryTestData.adminUser
        result = userQuerySetService.scan(user)
        def createdDiffsForFirstQuery = userQuerySetService.getDiffEntriesByQueryId(userQueryTestData.queries[0].id,
                user, 0, 999)

        then:
        result != null
        //check query_set_diffs entries
        result == 2
        createdDiffsForFirstQuery.size() == 4
        createdDiffsForFirstQuery.findAll{it.changeFlag == ChangeFlag.ADDED.toString()}.size() == 3
        createdDiffsForFirstQuery.findAll{it.changeFlag == ChangeFlag.REMOVED.toString()}.size() == 1

        //check number of added entries in total
        QuerySet.list().size() == userQueryTestData.querySets.size() + result //old set entries + entries created by scan
        QuerySetDiff.list().size() == 2 * createdDiffsForFirstQuery.size()    //both sets are the same
    }

}



