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
        hal          : ['application/hal+json', 'application/hal+xml'],
        xml          : ['text/xml', 'application/xml']
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

log4j = {

    warn 'org.codehaus.groovy.grails.web.servlet', // controllers
            'org.codehaus.groovy.grails.web.pages', // GSP
            'org.codehaus.groovy.grails.web.sitemesh', // layouts
            'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
            'org.codehaus.groovy.grails.web.mapping', // URL mapping
            'org.codehaus.groovy.grails.commons', // core / classloading
            'org.codehaus.groovy.grails.plugins', // plugins
            'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
            'org.codehaus.groovy.grails.domain', // domain classes
            'org.springframework',
            'org.hibernate',
            'net.sf.ehcache.hibernate'
    info 'org.transmartproject'
    debug 'org.hibernate.SQL'
    trace 'org.springframework.security',
            'grails.plugin.springsecurity'

    root {
        info('stdout')
    }
}

grails.converters.json.pretty.print = true
grails.plugin.springsecurity.active = false

environments {
    production {
        grails.converters.json.pretty.print = false
    }
}
