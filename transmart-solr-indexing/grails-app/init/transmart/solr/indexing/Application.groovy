package transmart.solr.indexing

import grails.boot.*
import grails.boot.config.GrailsAutoConfiguration
import grails.plugins.metadata.*

@PluginSource
class Application extends GrailsAutoConfiguration {
    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }

    @Override
    Collection<String> packageNames() {
        super.packageNames() + ['transmartproject.search']
    }
}
