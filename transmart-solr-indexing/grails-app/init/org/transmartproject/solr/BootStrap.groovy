package org.transmartproject.solr

import grails.util.Environment
import groovy.util.logging.Slf4j
import org.transmartproject.db.TestData
import org.transmartproject.db.user.AccessLevelTestData

@Slf4j
class BootStrap {

    def init = { servletContext ->
        if (Environment.current == Environment.TEST) {
            log.info "Setting up test data..."
            def testData = TestData.createDefault()
            testData.saveAll()
            AccessLevelTestData.createWithAlternativeConceptData(testData.conceptData)
                    .saveAll()
        }
    }

}
