
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
    cache.region.factory_class = 'org.hibernate.cache.ehcache.SingletonEhCacheRegionFactory'
    singleSession = true // configure OSIV singleSession mode
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

grails.mime.disable.accept.header.userAgents = []
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format

grails.views.default.codec = "none"

grails.controllers.defaultScope = 'singleton' //default is prototype

grails.converters.encoding = "UTF-8"
grails.converters.encoding = "UTF-8"
grails.converters.default.pretty.print = true

// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true

environments {
    test {
        grails.dbconsole.enabled = true
    }
}

environments {
    production {
        grails.converters.default.pretty.print = false
    }
}

dataSource {
    driverClassName = 'org.h2.Driver'
    url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;INIT=RUNSCRIPT FROM '../transmart-core-db/h2_init.sql'"
    dialect = 'org.hibernate.dialect.H2Dialect'
    username = 'sa'
    password = ''
    dbCreate = 'create'
    logSql = true
    formatSql = true
}
