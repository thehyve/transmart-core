package org.transmartproject.rest.data

import grails.transaction.Transactional
import groovy.util.logging.Slf4j

@Slf4j
@Transactional
class CurrentTestDataHolder {

    private TestData currentTestData

    boolean populateTestDataIfNeeded(TestData newTestData) {
        if (isTestDataDifferent(newTestData)) {
            log.info("Test data has changed. Start repopulating the database.")
            recreateTestData(newTestData)
            return true
        } else {
            log.info("The same test data has been used. No need to repopulate the database.")
            return false
        }
    }

    protected boolean isTestDataDifferent(TestData newTestData) {
        currentTestData != newTestData
    }

    void recreateTestData(TestData newTestData) {
        cleanCurrentData()
        newTestData.createTestData()
        currentTestData = newTestData
    }

    void cleanCurrentData() {
        if (currentTestData) {
            currentTestData.clearTestData()
        }
        currentTestData = null
    }
}
