package blend4j.plugin

import grails.converters.JSON
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

class Blend4jPluginController {

    /**
     *   Called to get the path to javascript resources such that the plugin can be loaded in the datasetExplorer
     */
    def loadScripts = {

        def scripts = [servletContext.contextPath + pluginContextPath + '/js/galaxyExport.js']

        def styles = []

        JSONObject result = new JSONObject()
        JSONArray rows = new JSONArray()

        for (file in scripts) {
            def m = [:]
            m['path'] = file.toString()
            m['type'] = 'script'
            rows.put(m)
        }

        for (file in styles) {
            def n = [:]
            n['path'] = file.toString()
            n['type'] = 'css'
            rows.put(n)
        }

        result.put('success', true)
        result.put('totalCount', scripts.size())
        result.put('files', rows)

        render result as JSON
    }
}
