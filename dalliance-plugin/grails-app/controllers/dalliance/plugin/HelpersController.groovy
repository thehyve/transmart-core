package dalliance.plugin

import org.codehaus.groovy.grails.plugins.GrailsPluginManager

class HelpersController {


    GrailsPluginManager pluginManager

    def index() {}

    /**
     * Get plugin path
     */
    def getPluginPath = {

        def pluginPath = pluginManager.getGrailsPlugin('dalliance-plugin').pluginPath
        response.outputStream << "$request.contextPath/static$pluginPath"

    }
}
