import grails.util.Environment
import groovy.util.logging.Log4j
import org.springframework.web.context.support.WebApplicationContextUtils

@Log4j
class BootStrap {

    def init = {servletContext ->
        Environment.executeForEnvironment(Environment.TEST, {
            def testData = Class.
                    forName('org.transmartproject.db.ontology.StudyTestData').newInstance()
            log.info 'About to save test data'
            testData.saveAll()
        })
    }
}
