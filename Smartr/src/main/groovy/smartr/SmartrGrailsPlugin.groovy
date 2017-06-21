package smartr

import grails.plugins.Plugin
import grails.util.Environment
import org.springframework.stereotype.Component
import groovy.util.logging.Slf4j
import org.springframework.web.context.support.ServletContextResource
import smartr.misc.SmartRRuntimeConstants
import smartr.rserve.RScriptsSynchronizer

@Slf4j
class SmartrGrailsPlugin extends Plugin {

    public static final String DEFAULT_REMOTE_RSCRIPTS_DIRECTORY = '/tmp/smart_r_scripts'
    public static final String TRANSMART_EXTENSIONS_REGISTRY_BEAN_NAME = 'transmartExtensionsRegistry'

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.1.10 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def title = "SmartR" // Headline display name of the plugin
    def author = "Sascha Herzinger"
    def authorEmail = "sascha.herzinger@uni.lu"
    def description = '''\
SmartR is a grails plugin seeking to improve the visual analytics of the tranSMART platform by using recent web technologies such as d3.
'''
    def profiles = ['web']

    // URL to the plugin's documentation
    def documentation = "https://github.com/transmart/SmartR"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
//    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]

    Closure doWithSpring() { {->
        // Scan for components
        xmlns context:"http://www.springframework.org/schema/context"

        context.'component-scan'('base-package': 'smartr') {
            context.'include-filter'(
                    type:       'annotation',
                    expression: Component.canonicalName)
        }
    }
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    private boolean skipRScriptsTransfer(config) {
        (!config.RModules.host ||
                config.RModules.host in ['127.0.0.1', '::1', 'localhost']) &&
                Environment.currentEnvironment == Environment.DEVELOPMENT &&
                !config.smartR.alwaysCopyScripts
    }

    private boolean copyResources(String root, File targetDirectory) {
        log.info "Copying resources from ${root} to ${targetDirectory.absolutePath} ..."
        def ctx = grailsApplication.getMainContext()
        def resources = ctx.getResources("${root}/**")
        try {
            if (!targetDirectory.exists()) {
                log.debug "Creating directory ${targetDirectory.absolutePath}"
                targetDirectory.mkdir()
            }
            for (res in resources) {
                def resource = res as ServletContextResource
                def targetPath = resource.path - root
                def target = new File(targetDirectory, targetPath)
                if (target.exists()) {
                    log.debug "Path already exists: ${target.absolutePath}"
                } else {
                    if (targetPath.endsWith('/')) {
                        log.debug "Creating directory ${target.absolutePath}"
                        target.mkdir()
                    } else {
                        target.createNewFile()
                        if (!target.canWrite()) {
                            log.error("File ${target.absolutePath} not writeable.")
                            return false
                        } else {
                            log.debug "Copying resource: ${resource.path} to ${target.absolutePath}"
                            target.withOutputStream { out_s ->
                                out_s << resource.inputStream
                                out_s.flush()
                            }
                        }
                    }
                }
            }
        } catch(IOException e) {
            log.error "Error while copying: ${e.message}"
            return false
        }
        return true
    }

    void doWithApplicationContext() {
        log.info "Initialising SmartR ..."

        def config = grailsApplication.config
        def ctx = grailsApplication.mainContext
        SmartRRuntimeConstants constants = ctx.getBean(SmartRRuntimeConstants)

        try { // find location of R scripts of SmartR

            def smartrScriptDir
            if (Environment.current == Environment.PRODUCTION) {
                def targetDirectory = new File(System.getProperty("user.home"), '.grails/smartr-rscripts')
                // FIXME: should the path be configurable?
                //targetDirectory = config.getOrDefault('smartr.deployment.rscripts', targetDirectory) as File
                if (copyResources('WEB-INF/smartr/Rscripts', targetDirectory)) {
                    smartrScriptDir = targetDirectory
                }
            } else {
                smartrScriptDir = new File('../Smartr/src/main/resources/Rscripts')
            }

            if (!smartrScriptDir || !smartrScriptDir.isDirectory()) {
                throw new RuntimeException('Could not determine proper for ' +
                        'Rscript directory')
            }

            constants.pluginScriptDirectory = smartrScriptDir
        } catch(Exception e) {
            log.warn "No location found for constants.pluginScriptDirectory: ${e.message}"
        }

        if (!skipRScriptsTransfer(config)) {
            def remoteScriptDirectory =  config.smartR.remoteScriptDirectory
            if (!remoteScriptDirectory) {
                remoteScriptDirectory = DEFAULT_REMOTE_RSCRIPTS_DIRECTORY
            }
            constants.remoteScriptDirectoryDir = remoteScriptDirectory
            log.info("Location for R scripts in the Rserve server is ${constants.remoteScriptDirectoryDir}")

            ctx.getBean(RScriptsSynchronizer).start()
        } else {
            log.info('Skipping copying of R script in development mode with local Rserve')
            constants.remoteScriptDirectoryDir = constants.pluginScriptDirectory.absoluteFile
            ctx.getBean(RScriptsSynchronizer).skip()
        }

        if (ctx.containsBean(TRANSMART_EXTENSIONS_REGISTRY_BEAN_NAME)) {
            ctx.getBean(TRANSMART_EXTENSIONS_REGISTRY_BEAN_NAME)
                    .registerAnalysisTabExtension('smartR', '/SmartR/loadScripts', 'addSmartRPanel')
        }

        log.info "SmartR initialised."
    }

    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
