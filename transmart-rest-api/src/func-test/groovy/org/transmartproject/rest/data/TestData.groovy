package org.transmartproject.rest.data

import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.transmartproject.core.config.SystemResource

import static org.transmartproject.db.utils.SessionUtils.*

@Slf4j
abstract class TestData {

    protected SessionFactory sessionFactory

    void clearTestData() {
        def currentSession = sessionFactory.currentSession
        truncateTables(currentSession, getAllTables(currentSession))
        resetIdSeq(currentSession, 'hibernate_sequence', 1)
    }

    abstract void createTestData()
}
