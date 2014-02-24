// configuration for plugin testing - will not be included in the plugin zip

/* Keep pre-2.3.0 behavior */
grails.databinding.convertEmptyStringsToNull = false
grails.databinding.trimStrings = false

///*
//Example configuration for using the reveng plugin
//grails.plugin.reveng.defaultSchema = 'DEAPP'
//grails.plugin.reveng.includeTables = ['DE_SUBJECT_METABOLOMICS_DATA', 'DE_METABOLITE_ANNOTATION']
//grails.plugin.reveng.packageName = 'org.transmartproject.db.dataquery.highdim.metabolite'
//*/

log4j = {

    warn 'org.codehaus.groovy.grails.commons.spring'
    warn 'org.codehaus.groovy.grails.domain.GrailsDomainClassCleaner'

    root {
        info('stdout')
    }
}

grails.converters.default.pretty.print=true

grails.views.default.codec="none" // none, html, base64
grails.views.gsp.encoding="UTF-8"
