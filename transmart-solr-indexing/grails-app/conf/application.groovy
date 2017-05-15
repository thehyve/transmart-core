
com.rwg.solr.scheme = 'http'
com.rwg.solr.host   = 'localhost:8983'
com.rwg.solr.facets.path = 'solr/facets/'
com.recomdata.FmFolderService.filestoreDirectory = (new File(System.getenv('HOME'), '.grails/transmart-filestore')).absolutePath

grails.codegen.defaultPackage = "org.transmartproject.solr"
grails{
    cache {
        order = 2000 // higher than default (1000) and plugins, usually 1500
        enabled = true
        clearAtStartup=true // reset caches when redeploying
        ehcache {
            ehcacheXmlLocation = 'classpath:ehcache.xml'
            reloadable = false
        }
    }
}

hibernate {
    cache.use_second_level_cache = false
    cache.use_query_cache = false
    cache.region.factory_class = 'org.hibernate.cache.SingletonEhCacheRegionFactory' // Hibernate 4
    singleSession = true // configure OSIV singleSession mode
}


// environment specific settings
environments {
    development {
        dataSources {
            dataSource {
                driverClassName = 'oracle.jdbc.driver.OracleDriver'
                url             = 'jdbc:oracle:thin:@dev5.thehyve.net:47526:ORCL'
                username        = 'biomart_user'
                password        = 'biomart_user'
                dbCreate        = 'none'
                logSql = true
                formatSql = true
                properties {
                    maxActive = 10
                    maxIdle = 5
                    minIdle = 2
                    initialSize = 2
                }
            }
        }
    }
    test {
        dataSource {
            driverClassName = 'org.h2.Driver'
            url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;INIT=RUNSCRIPT FROM '../transmart-core-db/h2_init.sql'"
            dialect = 'org.hibernate.dialect.H2Dialect'
            username = 'sa'
            password = ''
            dbCreate = 'create'
            logSql = false
            formatSql = true
        }
    }
}

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
//grails.spring.bean.packages = ["org.transmartproject.solr"]
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



