package org.transmartproject.db.userqueries

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.hibernate.SessionFactory
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.userquery.ChangeFlag
import org.transmartproject.db.TestData
import org.transmartproject.db.TransmartSpecification
import org.transmartproject.db.querytool.QuerySet
import org.transmartproject.db.querytool.QuerySetDiff
import org.transmartproject.db.querytool.QuerySetInstance

@Integration
@Rollback
class UserQuerySetServiceSpec extends TransmartSpecification {


    UserQuerySetService userQuerySetService

    SessionFactory sessionFactory

    UserQueryTestData userQueryTestData

    void setupData() {
        TestData.clearAllData()

        userQueryTestData = UserQueryTestData.createDefault()
        userQueryTestData.saveAll()
        sessionFactory.currentSession.flush()
    }

    void "test scanning for query set changes"() {
        setupData()

        when:'user is not admin'
        def user = userQueryTestData.user
        def result = userQuerySetService.scan(user)

        then:'AccessDeniedException is thrown'
        AccessDeniedException ex = thrown()
        ex.message == 'Only allowed for administrators.'

        when:'user is admin'
        def adminUser = userQueryTestData.adminUser
        result = userQuerySetService.scan(adminUser)
        def querySetChanges = userQuerySetService.getQueryChangeHistory(userQueryTestData.queries[0].id,
                user, 999)

        then:'number of updated queries equals 2'
        // check number of updated queries (number of created sets)
        result == 2
        // check query history
        querySetChanges.size() == 2

        //check number of added query_set and query_diff entries
        QuerySet.list().size() == userQueryTestData.querySets.size() + result //old sets + sets created by scan,

        def setInstances = QuerySetInstance.list()
        setInstances.size() == 12
        setInstances.count {it.querySet.id == 1} == 2
        setInstances.count {it.querySet.id == 2} == 2
        setInstances.count {it.querySet.id == 3} == 4 // 1. new set: two patients added
        setInstances.count {it.querySet.id == 4} == 4 // 2. new set: two patients added

        def setDiffs = QuerySetDiff.list()
        setDiffs.size() == 4
        setDiffs.count { it.changeFlag == ChangeFlag.ADDED } == 4 // 4 patients added in total
        setDiffs.count { it.changeFlag == ChangeFlag.REMOVED } == 0
    }

}



