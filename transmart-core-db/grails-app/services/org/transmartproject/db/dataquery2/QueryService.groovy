package org.transmartproject.db.dataquery2

import grails.transaction.Transactional
import org.hibernate.SessionFactory
import org.hibernate.criterion.DetachedCriteria
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.accesscontrol.AccessControlChecks
import org.transmartproject.db.dataquery2.query.HibernateCriteriaQueryBuilder
import org.transmartproject.db.dataquery2.query.ObservationQuery
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.user.User

@Transactional
class QueryService {

    @Autowired
    AccessControlChecks accessControlChecks

    SessionFactory sessionFactory

    List<ObservationFact> list(ObservationQuery query, User user) {
        def builder = new HibernateCriteriaQueryBuilder(
                studies: accessControlChecks.getDimensionStudiesForUser(user)
        )
        DetachedCriteria criteria = builder.detachedCriteriaFor(query)
        criteria.getExecutableCriteria(sessionFactory.currentSession).list()
    }

}
