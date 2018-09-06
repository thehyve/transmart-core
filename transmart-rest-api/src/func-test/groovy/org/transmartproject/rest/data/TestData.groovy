package org.transmartproject.rest.data

import grails.transaction.Transactional
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory

import static org.transmartproject.db.utils.SessionUtils.*

@Slf4j
abstract class TestData {

    protected SessionFactory sessionFactory

    @Transactional
    void clearTestData() {
        def currentSession = sessionFactory.currentSession
        truncateTables(currentSession, getAllTables(currentSession))
        resetIdSeq(currentSession, 'hibernate_sequence', 1)
    }

    abstract void createTestData()
}
