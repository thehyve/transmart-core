package com.recomdata.transmart.data.association

import org.json.JSONArray
import org.json.JSONObject

class DataAssociationController {
    
    def pluginService

    /**
     * Load the initial DataAssociation page.
     */
    def defaultPage =
    {
        render(template: "dataAssociation", model:[], contextPath:pluginContextPath)
    }

    /**
     *
     */
    def variableSelection = {
        def module = pluginService.findPluginModuleByModuleName(params.analysis)
        render(view:"../plugin/"+module.formPage)
    }

    /**
     * Load required scripts for running analysis
     */
    def loadScripts = {
        // list of required javascripts
        def scripts = [
                servletContext.contextPath + pluginContextPath + '/assets/FormValidator.js',
                servletContext.contextPath + pluginContextPath + '/assets/HighDimensionalData.js',
                servletContext.contextPath + pluginContextPath + '/assets/RmodulesView.js',
                servletContext.contextPath + pluginContextPath + '/assets/dataAssociation.js',
                servletContext.contextPath + pluginContextPath + '/assets/PDFGenerator.js',
                servletContext.contextPath + pluginContextPath + '/assets/ext/tsmart-overrides.js',
                servletContext.contextPath + pluginContextPath + '/assets/ext/tsmart-generic.js',
                servletContext.contextPath + pluginContextPath + '/assets/plugin/IC50.js']

        // list of required css
        def styles = [
                servletContext.contextPath+pluginContextPath+'/assets/rmodules.css',
                servletContext.contextPath+pluginContextPath+'/assets/jquery.qtip.min.css']
                //TODO: requires images: servletContext.contextPath+pluginContextPath+'/css/jquery-ui-1.10.3.custom.css']

        JSONObject result = new JSONObject()
        JSONArray rows = new JSONArray()
                
        // for all js files
        for (file in scripts) {
            def m = [:]
            m["path"] = file.toString()
            m["type"] = "script"
            rows.put(m);
        }

        // for all css files
        for (file in styles) {
            def n = [:]
            n["path"] = file.toString()
            n["type"] = "css"
            rows.put(n);
        }
        
        result.put("success", true)
        result.put("totalCount", scripts.size())
        result.put("files", rows)

        response.setContentType("text/json")
        response.outputStream << result.toString()
    }
}
