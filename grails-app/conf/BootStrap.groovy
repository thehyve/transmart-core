import grails.util.Environment
import groovy.util.logging.Log4j
import org.springframework.web.context.support.WebApplicationContextUtils

@Log4j
class BootStrap {

    def grailsApplication

    def init = {servletContext ->
        // Get spring
        def springContext = WebApplicationContextUtils.getWebApplicationContext(servletContext)

        // Force the bean being initialized
        springContext.getBean 'marshallersRegistrar'

        Environment.executeForEnvironment(Environment.TEST, {
            def testData = Class.
                    forName('org.transmartproject.db.ontology.StudyTestData').newInstance()
            log.info 'About to save test data'
            testData.saveAll()
        })
    }

    def destroy = {
    }
}
