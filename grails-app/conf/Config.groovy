// configuration for plugin testing - will not be included in the plugin zip

def dataSourceConfig = new File("${userHome}/" +
        ".grails/transmartConfig/DataSource.groovy")

if (!dataSourceConfig.exists())
    throw new RuntimeException("Coult not find ${dataSourceConfig}")

grails.config.locations = ["file:${dataSourceConfig.getAbsolutePath()}"]

grails.plugin.reveng.defaultSchema = 'i2b2metadata'
grails.plugin.reveng.includeTables = ['table_access']
grails.plugin.reveng.packageName = 'org.transmartproject.db.ontology'

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
grails.views.default.codec="none" // none, html, base64
grails.views.gsp.encoding="UTF-8"
