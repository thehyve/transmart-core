import grails.util.Environment
import org.springframework.web.context.support.WebApplicationContextUtils
import org.transmartproject.db.ontology.StudyTestData

class BootStrap {

    def grailsApplication

    def init = {servletContext ->
        // Get spring
        def springContext = WebApplicationContextUtils.getWebApplicationContext(servletContext)

        // Force the bean being initialized
        springContext.getBean 'marshallersRegistrar'

        Environment.executeForEnvironment(Environment.TEST, {
            StudyTestData testData = new StudyTestData()
            testData.saveAll()
        })
    }

    def destroy = {
    }
}
