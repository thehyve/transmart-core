
package com.thomsonreuters.lsps.transmart

class ThomsonReutersTagLib {
	def metacoreEnrichmentService
	
	def metacoreSettingsButton = {
		attrs, body ->
		
		def mode = metacoreEnrichmentService.metacoreSettingsMode()
		def settings = metacoreEnrichmentService.getMetacoreParams()
		
		out << render(template:'/metacoreEnrichment/metacoreSettingsButton', model: [settingsMode: mode, settings: settings], plugin: "transmart-metacore-plugin")
	}

    def metacoreEnrichmentResult = {
        attrs, body ->

        out << render(template:'/metacoreEnrichment/enrichmentResult', model: [prefix: 'marker_'], plugin: "transmart-metacore-plugin")
    }
}
