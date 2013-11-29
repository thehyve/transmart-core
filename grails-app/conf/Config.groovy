// configuration for plugin testing - will not be included in the plugin zip

def dataSourceConfig = new File("${userHome}/" +
        ".grails/transmartConfig/DataSource-coredb.groovy")

if (!dataSourceConfig.exists())
    throw new RuntimeException("Coult not find ${dataSourceConfig}")

grails.config.locations = ["file:${dataSourceConfig.getAbsolutePath()}"]

/* Keep pre-2.3.0 behavior */
grails.databinding.convertEmptyStringsToNull = false
grails.databinding.trimStrings = false

org.transmartproject.i2b2.user_id = 'i2b2'
org.transmartproject.i2b2.group_id = 'Demo'

/*
//Example configuration for using the reveng plugin
grails.plugin.reveng.defaultSchema = 'DEAPP'
grails.plugin.reveng.includeTables = ['DE_SUBJECT_RBM_DATA', 'DE_RBM_ANNOTATION']
grails.plugin.reveng.packageName = 'org.transmartproject.db.dataquery.highdim.rbm'
*/

log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}

    info  'org.codehaus.groovy.grails.web.servlet',  //  controllers
           'org.codehaus.groovy.grails.web.pages', //  GSP
           'org.codehaus.groovy.grails.web.sitemesh', //  layouts
           'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
           'org.codehaus.groovy.grails.web.mapping', // URL mapping
           'org.codehaus.groovy.grails.commons', // core / classloading
           'org.codehaus.groovy.grails.plugins', // plugins
           'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
           'org.springframework',
           'org.hibernate',
           'net.sf.ehcache.hibernate'

    info   'org.mortbay.log'

    root {
        info('stdout')
    }
}

grails.converters.default.pretty.print=true

grails.views.default.codec="none" // none, html, base64
grails.views.gsp.encoding="UTF-8"
