package com.recomdata.transmart.plugin
import grails.converters.JSON

class PluginModule {

    def pluginService

    long id
    String name
    String moduleName
    String params
    String version
    Boolean active
    Boolean hasForm
    String formLink
    String formPage
    PluginModuleCategory category

    static belongsTo = [plugin: Plugin]

    static mapping = {
        table 'SEARCHAPP.PLUGIN_MODULE'
        version false
        id column:'MODULE_SEQ',
                generator: 'sequence',
                params: [sequence:'SEARCHAPP.PLUGIN_MODULE_SEQ']
        active type:'yes_no'
        hasForm type:'yes_no'
        plugin column:'PLUGIN_SEQ'
        params lazy: true
        version false
    }

    static constraints = {
        name(nullable:false)
        moduleName(nullable:false, unique:true)
        active(nullable:false)
        hasForm(nullable:false)
        formLink(nullable:true)
        formPage(nullable:true)
    }

    def private setParamsStr(moduleParams) {
        if (moduleParams?.trim()) {
            def jsonObject = JSON.parse(moduleParams)
            params = jsonObject?.toString()
        }
    }

    def private getParamsStr() {
        return params
    }
}
