import grails.util.Environment
import org.springframework.aop.scope.ScopedProxyFactoryBean
import org.springframework.stereotype.Component
import org.transmartproject.rest.marshallers.MarshallersRegistrar

class TransmartRestApiGrailsPlugin {
    def version = "0.1"
    def grailsVersion = "2.3 > *"
    def title = "Transmart Rest Api Plugin"
    def author = "Transmart Foundation"
    def authorEmail = "admin@transmartproject.org"
    def description = '''\
        Plugin adds rest api to transmart applicaion
    '''

    def documentation = "https://wiki.thehyve.nl/"

    def organization = [name: "The Hyve", url: "http://www.thehyve.nl/"]

    def developers = [
            [name: "Ruslan Forostianov", email: "ruslan@thehyve.nl"],
            [name: "Jan Kanis", email: "jan@thehyve.nl"],
    ]

    def issueManagement = [system: "JIRA", url: "https://jira.thehyve.nl/browse/CHERKASY"]

    def scm = [url: "https://fisheye.ctmmtrait.nl/browse/transmart_rest_api"]

    def doWithSpring = {
        xmlns context: 'http://www.springframework.org/schema/context'

        context.'component-scan'('base-package': 'org.transmartproject.rest') {
            context.'include-filter'(
                    type: 'annotation',
                    expression: Component.canonicalName)
        }

        studyLoadingServiceProxy(ScopedProxyFactoryBean) {
            targetBeanName = 'studyLoadingService'
        }

        marshallersRegistrar(MarshallersRegistrar) {
            packageName = 'org.transmartproject.rest.marshallers'
        }
    }

    def doWithApplicationContext = { ctx ->  }
}
