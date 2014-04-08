import org.transmartproject.db.test.H2Views

class TransmartCoreDbTestsGrailsPlugin {
    def version = "1.0-LH-SNAPSHOT"
    def grailsVersion = "2.2 > *"

    def title = "Transmart Core Db Tests Plugin"
    def author = "Transmart Foundation"
    def authorEmail = "admin@transmartproject.org"
    def description = '''\
        The aim of this plugin is to reuse logic for populating db with test data.
        It also contains tests for core-db project to prevent circular
        plugin dependencies, which grails does not resolve.
    '''

    def documentation = "http://transmartproject.org"

    def scm = [ url: "https://fisheye.ctmmtrait.nl/browse/transmart_core_db" ]

    def developers = [
            [ name: "Ruslan Forostianov", email: "ruslan@thehyve.nl" ],
            [ name: "Peter Kok", email: "peter@thehyve.nl" ]
    ]

    def doWithSpring = {
        h2Views(H2Views)
    }
}
