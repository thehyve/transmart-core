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

    void "test fetching queryDiffs for a query"(){
        setupData()

        when:
        def user = userQueryTestData.user
        def result = userQueryDiffService.getAllByQueryId(userQueryTestData.queries[0].id, user, 0, 20)

        then:
        result != null
        result.size() == 2
    }


}
