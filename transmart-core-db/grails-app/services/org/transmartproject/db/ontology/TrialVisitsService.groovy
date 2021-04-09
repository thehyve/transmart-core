package org.transmartproject.db.ontology

import grails.plugin.cache.CacheEvict
import grails.plugin.cache.Cacheable
import grails.transaction.Transactional
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.hibernate.criterion.Order
import org.hibernate.criterion.Restrictions
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.db.i2b2data.TrialVisit

@Slf4j
@Transactional(readOnly = true)
class TrialVisitsService {

    @Autowired
    SessionFactory sessionFactory

    @Cacheable(value = 'org.transmartproject.db.ontology.TrialVisitsService', key = {study.name})
    List<TrialVisit> findTrialVisitsForStudy(MDStudy study) {
        def session = sessionFactory.openStatelessSession()
        try {
            def tx = session.beginTransaction()
            def trialVisits = session.createCriteria(TrialVisit)
                    .add(Restrictions.eq('study', study))
                    .addOrder(Order.asc('id'))
                    .list() as List<TrialVisit>
            tx.commit()
            println "Trial visits for study ${study.name} (${study.id}): ${trialVisits.collect { it.id }.toListString()}"
            return trialVisits
        } finally {
            session.close()
        }
    }

    @CacheEvict(value = 'org.transmartproject.db.ontology.TrialVisitsService', allEntries = true)
    void clearCache() {
        log.info 'Clearing trial visits cache ...'
    }

}
