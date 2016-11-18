class TransmartSolrIndexingGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.3 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Transmart Solr Indexing Plugin" // Headline display name of the plugin
    def author = "Your name"
    def authorEmail = ""
    def description = '''\
Indexes documents into solr.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/transmart-solr-indexing"

    def doWithWebDescriptor = { xml ->
    }

    def doWithSpring = {
        xmlns context: "http://www.springframework.org/schema/context"
        context.'component-scan'('base-package': 'org.transmartproject.search')
    }

    def doWithDynamicMethods = { ctx ->
    }

    def doWithApplicationContext = { ctx ->
    }

    def onChange = { event ->

    }

    def onConfigChange = { event ->
    }

    def onShutdown = { event ->
    }
}
