package org.transmartproject.rest.data

import groovy.util.logging.Slf4j

@Slf4j
abstract class TestData {

    void clearTestData() {
        log.info "Clear test data ..."
        org.transmartproject.db.TestData.clearAllData()
    }

    abstract void createTestData()

}
