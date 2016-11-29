package transmartproject.search

import grails.boot.*
import grails.boot.config.GrailsAutoConfiguration
import grails.plugins.metadata.*

@PluginSource
//@ComponentScan("transmartproject.search")
class Application extends GrailsAutoConfiguration {
    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }

    @Override
    Collection<String> packageNames() {
        super.packageNames() + ['transmartproject.search']
    }
}
