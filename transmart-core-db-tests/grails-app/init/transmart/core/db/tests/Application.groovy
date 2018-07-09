package transmart.core.db.tests

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import grails.util.Environment
import org.transmartproject.db.test.H2Views

class Application extends GrailsAutoConfiguration {
    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }

    @Override
    Closure doWithSpring() {
        if (Environment.currentEnvironment == Environment.TEST) {
            { ->
                h2Views(H2Views)
            }
        }
    }
}
