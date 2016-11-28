
com.rwg.solr.scheme = 'http'
com.rwg.solr.host   = 'localhost:6000'
com.rwg.solr.facets.path = 'solr/facets/'
com.recomdata.FmFolderService.filestoreDirectory = (new File(System.getenv('HOME'), '.grails/transmart-filestore')).absolutePath

//grails.plugin.reveng.packageName = 'org.transmartproject.search.browse'
//grails.plugin.reveng.includeTables = ['folder_study_mapping']
//grails.plugin.reveng.defaultSchema = 'biomart_user'

beans {
    cacheManager {
        shared = true
    }
}

hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = false
//    cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory' // Hibernate 3
    cache.region.factory_class = 'org.hibernate.cache.SingletonEhCacheRegionFactory' // Hibernate 4
    //cache.region.factory_class = 'grails.plugin.cache.ehcache.hibernate.BeanEhcacheRegionFactory4'
    singleSession = true // configure OSIV singleSession mode
}

// environment specific settings
environments {
    test {
        dataSource {
            driverClassName = 'org.postgresql.Driver'
            url = 'jdbc:postgresql://localhost:6000/transmart'
            username = 'biomart_user'
            password = 'biomart_user'
            dbCreate = 'none'
            logSql = true
            formatSql = true
        }
    }
}

/*
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

    //trace 'org.hibernate.type'
    //debug 'org.hibernate.SQL'
    debug 'org.transmartproject.search.indexing'
}
*/
grails.cache.config = {
    cache {
        name 'FacetsIndexCache'
        eternal false
        timeToLiveSeconds(15 * 60)
        maxElementsInMemory 10
        maxElementsOnDisk 0
    }
}
grails.plugin.springsecurity.active = false



grails.mime.types = [
        all          : '*/*',
        atom         : 'application/atom+xml',
        css          : 'text/css',
        csv          : 'text/csv',
        form         : 'application/x-www-form-urlencoded',
        html         : ['text/html', 'application/xhtml+xml'],
        js           : 'text/javascript',
        json         : ['application/json', 'text/json'],
        multipartForm: 'multipart/form-data',
        rss          : 'application/rss+xml',
        text         : 'text/plain',
        hal          : ['application/hal+json', 'application/hal+xml'],
        xml          : ['text/xml', 'application/xml'],
        protobuf     : 'application/x-protobuf',
]

grails.mime.use.accept.header = true

// Legacy setting for codec used to encode data with ${}
grails.views.default.codec = "html"

grails.controllers.defaultScope = 'singleton' //default is prototype

// GSP settings
grails {
    views {
        gsp {
            encoding = 'UTF-8'
            htmlcodec = 'xml' // use xml escaping instead of HTML4 escaping
            codecs {
                expression = 'html' // escapes values inside ${}
                scriptlet = 'html' // escapes output from scriptlets in GSPs
                taglib = 'none' // escapes output from taglibs
                staticparts = 'none'
                // escapes output from static template parts
            }
        }
        // escapes all not-encoded output at final stage of outputting
        filteringCodecForContentType {
            //'text/html' = 'html'
        }
    }
}

grails.converters.encoding = "UTF-8"
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = []
// whether to disable processing of multi part requests
grails.web.disable.multipart = true



environments {
    test {
        grails.dbconsole.enabled = true
    }
}

grails.converters.json.pretty.print = true

grails.mime.disable.accept.header.userAgents = false

environments {
    production {
        grails.converters.json.pretty.print = false
    }
}




