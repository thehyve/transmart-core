import grails.util.Environment
import groovy.util.logging.Log4j
import org.springframework.web.context.support.WebApplicationContextUtils

import org.codehaus.groovy.grails.test.runner.phase.IntegrationTestPhaseConfigurer

@Log4j
class BootStrap {

    def init = {servletContext ->
        // Get spring
        def springContext = WebApplicationContextUtils.getWebApplicationContext(servletContext)

        // Force the bean being initialized
        springContext.getBean 'marshallersRegistrar'

        Environment.executeForEnvironment(Environment.TEST, {
            if (IntegrationTestPhaseConfigurer.currentApplicationContext) {
                /* don't load the test data bundle for integration tests */
                return
            }

            def testData = Class.
                    forName('org.transmartproject.db.ontology.StudyTestData').newInstance()
            log.info 'About to save test data'
            testData.saveAll()
        })
    }

    def destroy = {
    }
}
