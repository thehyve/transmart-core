// configuration for plugin testing - will not be included in the plugin zip

def dataSourceConfig = new File("${userHome}/" +
        ".grails/transmartConfig/DataSource-coredb.groovy")

if (dataSourceConfig.exists()) {
    grails.config.locations = ["file:${dataSourceConfig.getAbsolutePath()}"]
}

grails.databinding.convertEmptyStringsToNull = false
grails.databinding.trimStrings = false

org.transmartproject.i2b2.user_id = 'i2b2'
org.transmartproject.i2b2.group_id = 'Demo'

log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}

    error  'org.codehaus.groovy.grails.web.servlet',  //  controllers
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
}
