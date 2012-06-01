/*************************************************************************   
* Copyright 2008-2012 Janssen Research & Development, LLC.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
******************************************************************/

package com.recomdata.transmart.data.association
import org.json.*

class DataAssociationController {
	
	def dataAssociationService
	def pluginService

	/**
	 * Display the initial DataAssociation page.
	 */
	def defaultPage =
	{
		//def modules = pluginService.getPluginModules('R-Modules')
		render(template: "dataAssociation", model:[], contextPath:pluginContextPath)
	}
	
	def variableSelection = {
		def module = pluginService.findPluginModuleByModuleName(params.analysis)
		render(view:"../plugin/"+module.formPage)
	}
	
	def loadScripts = {
		def scripts = [servletContext.contextPath+pluginContextPath+'/js/dataAssociation.js', 
		servletContext.contextPath+pluginContextPath+'/js/PDFGenerator.js',
		servletContext.contextPath+pluginContextPath+'/js/plugin/SurvivalAnalysis.js',
		servletContext.contextPath+pluginContextPath+'/js/plugin/CorrelationAnalysis.js',
		servletContext.contextPath+pluginContextPath+'/js/plugin/LineGraph.js',
		servletContext.contextPath+pluginContextPath+'/js/plugin/ScatterPlot.js',
		servletContext.contextPath+pluginContextPath+'/js/plugin/BoxPlot.js',
		servletContext.contextPath+pluginContextPath+'/js/plugin/TableWithFisher.js',
		servletContext.contextPath+pluginContextPath+'/js/plugin/Heatmap.js',
		servletContext.contextPath+pluginContextPath+'/js/plugin/HClust.js',
		servletContext.contextPath+pluginContextPath+'/js/plugin/KClust.js',
		servletContext.contextPath+pluginContextPath+'/js/plugin/Waterfall.js',
		servletContext.contextPath+pluginContextPath+'/js/plugin/BoxPlot.js',
		servletContext.contextPath+pluginContextPath+'/js/plugin/IC50.js',
		servletContext.contextPath+pluginContextPath+'/js/plugin/PCA.js',
		servletContext.contextPath+pluginContextPath+'/js/plugin/MarkerSelection.js']
		

		
		JSONObject result = new JSONObject()
		JSONArray rows = new JSONArray()
		
		for (file in scripts) {
			def m = [:]
			m["path"] = file.toString()
			m["type"] = "script"
			rows.put(m);
		}
		
		result.put("success", true)
		result.put("totalCount", scripts.size())		
		result.put("files", rows)
		
		response.setContentType("text/json")
		response.outputStream << result.toString()
	}
}