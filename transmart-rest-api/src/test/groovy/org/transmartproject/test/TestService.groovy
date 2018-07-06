package org.transmartproject.test

import grails.transaction.Transactional
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.hibernate.criterion.DetachedCriteria
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.TestData
import org.transmartproject.db.ontology.I2b2Secure
import org.transmartproject.db.querytool.QtQueryResultType
import org.transmartproject.db.test.H2DatabaseCreator
import org.transmartproject.db.user.AccessLevelTestData
import org.transmartproject.rest.TestResource

@Slf4j
@CompileStatic
class TestService implements TestResource {

    @Autowired
    SessionFactory sessionFactory

    @Transactional
    void createTestData() {
        log.info "Setup test data ..."
        def session = sessionFactory.currentSession
        // Check if dictionaries were already loaded before
        def resultTypes = DetachedCriteria.forClass(QtQueryResultType).getExecutableCriteria(session).list() as List<QtQueryResultType>
        if (resultTypes.size() == 0) {
            log.info "Setup test database"
            H2DatabaseCreator.initDatabase()
        }
        // Check if test data has been created before
        def nodes = DetachedCriteria.forClass(I2b2Secure).getExecutableCriteria(session).list() as List<I2b2Secure>
        if (nodes.size() == 0) {
            log.info "Create test data"
            def testData = TestData.createDefault()
            testData.saveAll()
            new org.transmartproject.rest.TestData().createTestData()
            def accessLevelTestData = AccessLevelTestData.createWithAlternativeConceptData(testData.conceptData)
            accessLevelTestData.saveAll()
        }
    }

}
