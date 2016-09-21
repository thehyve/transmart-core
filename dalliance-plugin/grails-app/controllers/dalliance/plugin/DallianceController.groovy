package dalliance.plugin

import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

class DallianceController {

    def index() {
        render (view: "main")
    }

    def loadScripts = {
        def scripts = [
            servletContext.contextPath+pluginContextPath+'/build/dalliance-all.js',
            servletContext.contextPath+pluginContextPath+'/dalliance.js',
        ]

        JSONObject result = new JSONObject()
        JSONArray rows = new JSONArray()

        for (file in scripts) {

            JSONObject aScript = new JSONObject()
            aScript.put("path", file.toString())
            aScript.put("type", "script")
            rows.put(aScript)
        }

        result.put("success", true)
        result.put("totalCount", scripts.size())
        result.put("files", rows)

        response.setContentType("text/json")
        response.outputStream << result.toString()
    }
}
