
/*
 * Copyright 2014 Janssen Research & Development, LLC.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version, along with the following terms:
 *
 *   1. You may convey a work based on this program in accordance with
 *      section 5, provided that you retain the above notices.
 *   2. You may convey verbatim copies of this program code as you receive
 *      it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

grails.databinding.convertEmptyStringsToNull = false
grails.databinding.trimStrings = false

grails.config.locations = []
def defaultConfigFiles = [
        "${userHome}/.grails/transmartConfig/DataSource-rest-api.groovy"
]
defaultConfigFiles.each {filePath ->
    def f = new File(filePath)
    if (f.exists()) {
        println "[INFO] Including configuration file '$f' in configuration building."
        grails.config.locations << "file:${filePath}"
    } else {
        println "[INFO] Configuration file '$f' not found."
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
        hal          : 'application/hal+json',
        halxml       : 'application/hal+xml',
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

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password']

// configure auto-caching of queries by default (if false you can cache individual queries with 'cache: true')
grails.hibernate.cache.queries = false

environments {
    test {
        grails.dbconsole.enabled = true
    }
}

environments {
    development {
        grails.logging.jul.usebridge = true
    }
    production {
        grails.logging.jul.usebridge = false
    }
}

grails.converters.json.pretty.print = true
grails.plugin.springsecurity.active = false

grails.mime.disable.accept.header.userAgents = false

environments {
    production {
        grails.converters.json.pretty.print = false
    }
}

hibernate {
    cache.use_second_level_cache = false
    cache.use_query_cache = false
}

environments {
    test {
        dataSource {
            driverClassName = 'org.h2.Driver'
            url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;INIT=RUNSCRIPT FROM '../transmart-core-db/h2_init.sql'"
            dialect = 'org.hibernate.dialect.H2Dialect'
            username = 'sa'
            password = ''
            dbCreate = 'create-drop'
            logSql = false
            formatSql = true
        }
    }
    development {
        dataSource {
            url = 'jdbc:postgresql://localhost:5433/transmart'
            driverClassName = 'org.postgresql.Driver'
            username = 'tm_cz'
            password = 'tm_cz'
            logSql = true
            formatSql = true
        }
    }
}
// vim: set ts=4 sw=4 et:
