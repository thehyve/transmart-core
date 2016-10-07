package org.transmartproject.db.dataquery2

import grails.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.accesscontrol.AccessControlChecks
import org.transmartproject.db.dataquery2.query.CriteriaQueryBuilder
import org.transmartproject.db.dataquery2.query.ObservationQuery
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.user.User

@Transactional
class QueryService {

    @Autowired
    AccessControlChecks accessControlChecks

    List<ObservationFact> list(ObservationQuery query, User user) {
        def builder = new CriteriaQueryBuilder(
                studies: accessControlChecks.getDimensionStudiesForUser(user)
        )
        builder.build(query).list()
    }

}
