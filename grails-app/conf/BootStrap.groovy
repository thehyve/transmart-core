import grails.util.Environment
import groovy.util.logging.Log4j
import org.springframework.web.context.support.WebApplicationContextUtils

import org.codehaus.groovy.grails.test.runner.phase.IntegrationTestPhaseConfigurer

@Log4j
class BootStrap {
    def init = {servletContext ->
        Environment.executeForEnvironment(Environment.TEST, {
            if (IntegrationTestPhaseConfigurer.currentApplicationContext) {
                /* don't load the test data bundle for integration tests */
                return
            }
            def testData = createTestData()
            log.info 'About to save test data'
            testData.saveAll()
        })
    }

    def createTestData() {
        Class clazz = Class.forName('org.transmartproject.db.TestData')
        clazz.getMethod('createDefault').invoke(null) //static method
    }
}
