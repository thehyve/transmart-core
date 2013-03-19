import org.transmartproject.db.http.BusinessExceptionResolver
import org.transmartproject.db.support.MarshallerRegistrarService

class TransmartCoreGrailsPlugin {
    // the plugin version
    def version = "1.0-SNAPSHOT"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.2 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def title = "Transmart Core DB Plugin"
    def author = "Transmart Foundation"
    def authorEmail = "admin@transmartproject.org"
    def description = '''\
A runtime dependency for tranSMART that implements the Core API
'''

    // URL to the plugin's documentation
    def documentation = "http://transmartproject.org"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
//    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
    def developers = [
            [ name: "Kees van Bochove",  email: "kees@thehyve.nl"],
            [ name: "Gustavo Lopes"   ,  email: "gustavo@thehyve.nl" ],
    ]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
    def scm = [ url: "https://fisheye.ctmmtrait.nl/browse/transmart_core_db" ]

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before
    }

    def doWithSpring = {
        businessExceptionResolver(BusinessExceptionResolver)
    }

    def doWithDynamicMethods = { ctx ->
        String.metaClass.asLikeLiteral = { replaceAll(/[\\%_]/, '\\\\$0') }
    }

    def doWithApplicationContext = { applicationContext ->
        MarshallerRegistrarService bean =
            applicationContext.getBean(MarshallerRegistrarService)
        bean.scanForClasses(applicationContext)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    def onShutdown = { event ->
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
