import test.SpringSecurityOAuth2Api


grails.project.groupId = appName

grails.mime.types = [
    all:           '*/*',
    atom:          'application/atom+xml',
    css:           'text/css',
    csv:           'text/csv',
    form:          'application/x-www-form-urlencoded',
    html:          ['text/html','application/xhtml+xml'],
    js:            'text/javascript',
    json:          ['application/json', 'text/json'],
    multipartForm: 'multipart/form-data',
    rss:           'application/rss+xml',
    text:          'text/plain',
    hal:           ['application/hal+json','application/hal+xml'],
    xml:           ['text/xml', 'application/xml']
]

// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// What URL patterns should be processed by the resources plugin
grails.resources.adhoc.patterns = ['/images/*', '/css/*', '/js/*', '/plugins/*']

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
                staticparts = 'none' // escapes output from static template parts
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
grails.web.disable.multipart=false

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password']

// configure auto-caching of queries by default (if false you can cache individual queries with 'cache: true')
grails.hibernate.cache.queries = false

environments {
    development {
        grails.logging.jul.usebridge = true
    }
    production {
        grails.logging.jul.usebridge = false
    }
}

log4j = {

    warn   'org.codehaus.groovy.grails.web.servlet',        // controllers
           'org.codehaus.groovy.grails.web.pages',          // GSP
           'org.codehaus.groovy.grails.web.sitemesh',       // layouts
           'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
           'org.codehaus.groovy.grails.web.mapping',        // URL mapping
           'org.codehaus.groovy.grails.commons',            // core / classloading
           'org.codehaus.groovy.grails.plugins',            // plugins
           'org.codehaus.groovy.grails.orm.hibernate',      // hibernate integration
           'org.codehaus.groovy.grails.domain',            // domain classes
           'org.springframework',
           'org.hibernate',
           'net.sf.ehcache.hibernate'
    info   'org.transmartproject'
    debug  'org.hibernate.SQL'
    trace  'org.springframework.security',
           'grails.plugin.springsecurity'

    root {
        info('stdout')
    }
}

grails { plugin { springsecurity {

    /* the oauth should be moved away to transmartApp */
    oauthProvider {
        clients = [
                [clientId: "myId", clientSecret: "mySecret"]
        ]
    }

    securityConfigType = 'InterceptUrlMap' // no need for controller annotations method
    interceptUrlMap  = [
            '/login/**':                 ['permitAll'],
            '/logout/**':                ['permitAll'],
            '/oauth/authorize.dispatch': ['IS_AUTHENTICATED_REMEMBERED'],
            '/oauth/token.dispatch':     ['IS_AUTHENTICATED_REMEMBERED'],
            '/js/**':                    ['permitAll'],
            '/css/**':                   ['permitAll'],
            '/images/**':                ['permitAll'],
            '/static/**':                ['permitAll'],
            '/favicon.ico':              ['permitAll'],
            '/**':                       ['IS_AUTHENTICATED_REMEMBERED']
    ]

    oauthProvider.active = true // Set oauth provider active for test as well

    /* this should removed when authentication is moved to transmartApp */
    userLookup.userDomainClassName    = 'auth.AuthUser'
    userLookup.authorityJoinClassName = 'auth.UserUser'
    authority.className               = 'auth.Role'

    password.algorithm        = 'bcrypt'
    password.bcrypt.logrounds = 14
    password.hash.iterations  = 1

    providerNames = [
            'daoAuthenticationProvider',
            'anonymousAuthenticationProvider',
            'rememberMeAuthenticationProvider',
            'clientCredentialsAuthenticationProvider' //oauth
    ]

} } }

oauth {
    providers {
        // should be transmartApp instead
        mine {
            api      = SpringSecurityOAuth2Api
            key      = 'myId'
            secret   = 'mySecret'
            callback = 'http://localhost:8080/transmart-rest-api/studies/'
        }
    }
    debug = true
}

grails.converters.json.pretty.print = true

environments { production {
    grails.converters.json.pretty.print = false
} }
