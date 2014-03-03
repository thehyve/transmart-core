package dalliance.plugin

import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class DallianceController {

    def index() {
        render (view: "main")
    }

    def loadScripts = {
        def scripts = [
                servletContext.contextPath+pluginContextPath+'/js/tsmart-utils.js',
                servletContext.contextPath+pluginContextPath+'/js/bam.js',
                servletContext.contextPath+pluginContextPath+'/js/bigwig.js',
                servletContext.contextPath+pluginContextPath+'/js/das.js',
                servletContext.contextPath+pluginContextPath+'/js/spans.js',
                servletContext.contextPath+pluginContextPath+'/js/utils.js',
                servletContext.contextPath+pluginContextPath+'/js/cbrowser.js',
                servletContext.contextPath+pluginContextPath+'/js/feature-popup.js',
                servletContext.contextPath+pluginContextPath+'/js/tier.js',
                servletContext.contextPath+pluginContextPath+'/js/features.js',
                servletContext.contextPath+pluginContextPath+'/js/color.js',
                servletContext.contextPath+pluginContextPath+'/js/feature-draw.js',
                servletContext.contextPath+pluginContextPath+'/js/sequence-draw.js',
                servletContext.contextPath+pluginContextPath+'/js/domui.js',
                servletContext.contextPath+pluginContextPath+'/js/karyoscape.js',
                servletContext.contextPath+pluginContextPath+'/js/quant-config.js',
                servletContext.contextPath+pluginContextPath+'/js/track-adder.js',
                servletContext.contextPath+pluginContextPath+'/js/track-adder-custom.js',
                servletContext.contextPath+pluginContextPath+'/js/chainset.js',
                servletContext.contextPath+pluginContextPath+'/js/version.js',
                servletContext.contextPath+pluginContextPath+'/js/sha1.js',
                servletContext.contextPath+pluginContextPath+'/js/sample.js',
                servletContext.contextPath+pluginContextPath+'/js/kspace.js',
                servletContext.contextPath+pluginContextPath+'/json/json2.js',
                servletContext.contextPath+pluginContextPath+'/js/bin.js',
                servletContext.contextPath+pluginContextPath+'/js/twoBit.js',
                servletContext.contextPath+pluginContextPath+'/js/thub.js',
                servletContext.contextPath+pluginContextPath+'/js/svg-export.js',
                servletContext.contextPath+pluginContextPath+'/jszlib/js/inflate.js',
                servletContext.contextPath+pluginContextPath+'/js/browser-ui.js',
                servletContext.contextPath+pluginContextPath+'/js/glyphs.js',
                servletContext.contextPath+pluginContextPath+'/js/session.js',
                servletContext.contextPath+pluginContextPath+'/js/jbjson.js',
                servletContext.contextPath+pluginContextPath+'/js/sourceadapters.js',
                servletContext.contextPath+pluginContextPath+'/polyfills/html5slider.js',
                servletContext.contextPath+pluginContextPath+'/js/ensembljson.js',
                servletContext.contextPath+pluginContextPath+'/js/overlay.js',
                servletContext.contextPath+pluginContextPath+'/js/tier-actions.js',
                servletContext.contextPath+pluginContextPath+'/dalliance.js'
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
