/**
 * Running externalized configuration
 * Assuming the following configuration files
 * - in the executing user's home at ~/.grails/<app_name>Config/[application.groovy|Config.groovy|DataSource.groovy]
 * - config location set path by system variable '<APP_NAME>_CONFIG_LOCATION'
 * - dataSource location set path by system environment variable '<APP_NAME>_DATASOURCE_LOCATION'
 */

/* For some reason, the externalized config files are run with a different
 * binding. None of the variables appName, userHome, appVersion, grailsHome
 * are available; the binding will actually be the root config object.
 * So store the current binding in the config object so the externalized
 * config has access to the variables mentioned.
 */
org.transmart.originalConfigBinding = getBinding()

org.transmartproject.app.oauthEnabled = true
org.transmartproject.app.tmAppCompiled = true
org.transmartproject.app.transmartURL = "http://localhost:${System.getProperty('server.port', '8080')}"

grails.assets.bundle = true

grails.resources.pattern = '/**'

server.compression.enabled = true
server.compression.mimeTypes = ['application/json', 'application/xml', 'text/html', 'text/xml', 'text/plain']

grails.mime.disable.accept.header.userAgents = []
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.types = [html         : [
        'text/html',
        'application/xhtml+xml'
],
                     xml          : [
                             'text/xml',
                             'application/xml'
                     ],
                     text         : 'text-plain',
                     js           : 'text/javascript',
                     rss          : 'application/rss+xml',
                     atom         : 'application/atom+xml',
                     css          : 'text/css',
                     csv          : 'text/csv',
                     all          : '*/*',
                     json         : [
                             'application/json',
                             'text/json'
                     ],
                     form         : 'application/x-www-form-urlencoded',
                     multipartForm: 'multipart/form-data',
                     jnlp         : 'application/x-java-jnlp-file',
                     protobuf     : 'application/x-protobuf',
]

//Sets upload file size limit
grails.controllers.upload.maxFileSize = 10 * 1024 * 1024 //10mb
grails.controllers.upload.maxRequestSize = 10 * 1024 * 1024

// The default codec used to encode data with ${}
grails.views.javascript.library="jquery"
grails.views.default.codec = "none" // none, html, base64
grails.views.gsp.encoding = "UTF-8"
grails.converters.encoding = "UTF-8"
grails.converters.default.pretty.print = true

/* Keep pre-2.3.0 behavior */
grails.databinding.convertEmptyStringsToNull = false
grails.databinding.trimStrings = false

// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true

com.recomdata.skipdisclaimer = true

//core-db settings
org.transmartproject.i2b2.user_id = 'i2b2'
org.transmartproject.i2b2.group_id = 'Demo'
//**************************

/* {{{ TRANSMARTAPP PLUGIN SPECIFIC CONFIGURATION */
// Remaining settings are a part of transmart-server configuration

com.recomdata.search.autocomplete.max = 20
// default paging size
com.recomdata.search.paginate.max = 20
com.recomdata.search.paginate.maxsteps = 5
com.recomdata.admin.paginate.max = 20

//**************************
//This is the login information for the different i2b2 projects.
//SUBJECT Data.
com.recomdata.i2b2.subject.domain = 'i2b2demo'
com.recomdata.i2b2.subject.projectid = 'i2b2demo'
com.recomdata.i2b2.subject.username = 'Demo'
com.recomdata.i2b2.subject.password = 'demouser'

//SAMPLE Data.
com.recomdata.i2b2.sample.domain = 'i2b2demo'
com.recomdata.i2b2.sample.projectid = 'i2b2demo'
com.recomdata.i2b2.sample.username = 'sample'
com.recomdata.i2b2.sample.password = 'manager'

//**************************

// max genes to display after disease search
com.recomdata.search.gene.max = 250;

// set schema names for I2B2HelperService
com.recomdata.i2b2helper.i2b2hive = "i2b2hive"
com.recomdata.i2b2helper.i2b2metadata = "i2b2metadata"
com.recomdata.i2b2helper.i2b2demodata = "i2b2demodata"

com.recomdata.transmart.data.export.max.export.jobs.loaded = 20

com.recomdata.transmart.data.export.dataTypesMap = [
        'CLINICAL'  : 'Clinical & Low Dimensional Biomarker Data',
        'MRNA'      : 'Gene Expression Data',
        'SNP'       : 'SNP data (Microarray)',
        'STUDY'     : 'Study Metadata',
        'ADDITIONAL': 'Additional Data'
        //,'GSEA':'Gene Set Enrichment Analysis (GSEA)'
];

// Data export FTP settings is Rserve running remote in relation to transmartApp
com.recomdata.transmart.data.export.ftp.server = ''
com.recomdata.transmart.data.export.ftp.serverport = ''
com.recomdata.transmart.data.export.ftp.username = ''
com.recomdata.transmart.data.export.ftp.password = ''
com.recomdata.transmart.data.export.ftp.remote.path = ''

// Control which gene/pathway search is used in Dataset Explorer
// A value of "native" forces Dataset Explorer's native algorithm.
// Abscence of this property or any other value forces the use of the Search Algorithm
//com.recomdata.search.genepathway="native"

// The tags in the Concept to indicate Progression-free Survival and Censor flags, used by Survival Analysis
com.recomdata.analysis.survival.survivalDataList = [
        '(PFS)',
        '(OS)',
        '(TTT)',
        '(DURTFI)'
];
com.recomdata.analysis.survival.censorFlagList = [
        '(PFSCENS)',
        '(OSCENS)',
        '(TTTCENS)',
        '(DURTFICS)'
];

com.recomdata.analysis.genepattern.file.dir = "data"; // Relative to the app root "web-app" - deprecated - replaced with data.file.dir

com.recomdata.analysis.data.file.dir = "data"; // Relative to the app root "web-app"

// Directories to write R scripts to for use by RServe. Resources are copied at startup.
org.transmartproject.rmodules.deployment.rscripts = new File(System.getProperty("user.home"), '.grails/transmart-rscripts')
org.transmartproject.rmodules.deployment.dataexportRscripts = new File(System.getProperty("user.home"), '.grails/transmart-dataexport-rscripts')

// Disclaimer
StringBuilder disclaimer = new StringBuilder()
disclaimer.append("<p></p>")
com.recomdata.disclaimer = disclaimer.toString()

// customization views
//com.recomdata.view.studyview='_clinicaltrialdetail'
com.recomdata.skipdisclaimer = true

grails.spring.bean.packages = []

grails.spring.transactionManagement.proxies = true

org.transmart.security.spnegoEnabled = false
org.transmart.security.sniValidation = true
org.transmart.security.sslValidation = true

grails.plugin.springsecurity.useSecurityEventListener = true

bruteForceLoginLock {
    allowedNumberOfAttempts = 3
    lockTimeInMinutes = 10
}

grails {
    cache {
        enabled = true
        ehcache {
            ehcacheXmlLocation = 'classpath:ehcache.xml'
            reloadable = false
        }
    }
}

// Added by the Spring Security OAuth2 Provider plugin:
grails.plugin.springsecurity.oauthProvider.clientLookup.className = 'org.transmart.oauth.Client'
grails.plugin.springsecurity.oauthProvider.authorizationCodeLookup.className = 'org.transmart.oauth.AuthorizationCode'
grails.plugin.springsecurity.oauthProvider.accessTokenLookup.className = 'org.transmart.oauth.AccessToken'
grails.plugin.springsecurity.oauthProvider.refreshTokenLookup.className = 'org.transmart.oauth.RefreshToken'

// Disable LDAP by default to prevent authentication errors for installations without LDAP
grails.plugin.springsecurity.ldap.active = false
org.transmart.security.ldap.mappedUsernameProperty = 'username'
org.transmart.security.ldap.inheritPassword = true

/* {{{ Spring Security configuration */

grails { plugin { springsecurity {

    // customized user GORM class
    userLookup.userDomainClassName = 'org.transmart.searchapp.AuthUser'
    // customized password field
    userLookup.passwordPropertyName = 'passwd'
    // customized user /role join GORM class
    userLookup.authorityJoinClassName = 'org.transmart.searchapp.AuthUser'
    // customized role GORM class
    authority.className = 'org.transmart.searchapp.Role'
    // request map GORM class name - request map is stored in the db
    requestMap.className = 'org.transmart.searchapp.Requestmap'
    // requestmap in db
    securityConfigType = grails.plugin.springsecurity.SecurityConfigType.Requestmap
    // url to redirect after login in
    successHandler.defaultTargetUrl = '/userLanding'
    // logout url
    logout.afterLogoutUrl = '/login/forceAuth'

    // configurable requestmap functionality in transmart is deprecated
    def useRequestMap = false

    if (useRequestMap) {
        // requestmap in db
        securityConfigType = 'Requestmap'
        // request map GORM class name - request map is stored in the db
        requestMap.className = 'org.transmart.searchapp.Requestmap'
    } else {
        securityConfigType = 'InterceptUrlMap'
        def oauthEndpoints = [
                [pattern: '/oauth/authorize.dispatch', access: ["isFullyAuthenticated() and request.getMethod().equals('POST')"]],
                [pattern: '/oauth/token.dispatch', access: ["isFullyAuthenticated() and (request.getMethod().equals('GET') or request.getMethod().equals('POST'))"]],
        ]

        interceptUrlMap = [
                [pattern: '/login/**',                   access: ['IS_AUTHENTICATED_ANONYMOUSLY']],
                [pattern: '/js/**',                      access: ['IS_AUTHENTICATED_ANONYMOUSLY']],
                [pattern: '/css/**',                     access: ['IS_AUTHENTICATED_ANONYMOUSLY']],
                [pattern: '/assets/**',                  access: ['IS_AUTHENTICATED_ANONYMOUSLY']],
                [pattern: '/grails-errorhandler',        access: ['IS_AUTHENTICATED_ANONYMOUSLY']],
                [pattern: '/images/analysisFiles/**',    access: ['IS_AUTHENTICATED_REMEMBERED']],
                [pattern: '/images/**',                  access: ['IS_AUTHENTICATED_ANONYMOUSLY']],
                [pattern: '/static/**',                  access: ['IS_AUTHENTICATED_ANONYMOUSLY']],
                [pattern: '/search/loadAJAX**',          access: ['IS_AUTHENTICATED_ANONYMOUSLY']],
                [pattern: '/analysis/getGenePatternFile',access: ['IS_AUTHENTICATED_ANONYMOUSLY']],
                [pattern: '/analysis/getTestFile',       access: ['IS_AUTHENTICATED_ANONYMOUSLY']],
                [pattern: '/open-api/**',                access: ['IS_AUTHENTICATED_ANONYMOUSLY']],
                [pattern: '/requestmap/**',              access: ['ROLE_ADMIN']],
                [pattern: '/role/**',                    access: ['ROLE_ADMIN']],
                [pattern: '/authUser/**',                access: ['ROLE_ADMIN']],
                [pattern: '/secureObject/**',            access: ['ROLE_ADMIN']],
                [pattern: '/accessLog/**',               access: ['ROLE_ADMIN']],
                [pattern: '/authUserSecureAccess/**',    access: ['ROLE_ADMIN']],
                [pattern: '/secureObjectPath/**',        access: ['ROLE_ADMIN']],
                [pattern: '/userGroup/**',               access: ['ROLE_ADMIN']],
                //TODO This looks dangerous. It opens acess to the gwas data for everybody.
                [pattern: '/gwasWeb/**',                 access: ['IS_AUTHENTICATED_ANONYMOUSLY']],
                [pattern: '/oauthAdmin/**',              access: ['ROLE_ADMIN']],
                [pattern: '/secureObjectAccess/**',      access: ['ROLE_ADMIN']]
        ] +
                (org.transmartproject.app.oauthEnabled ?  oauthEndpoints : []) +
                [
                        [pattern: '/**',                         access: ['IS_AUTHENTICATED_REMEMBERED']], // must be last
                ]
        rejectIfNoRule = true
    }

    // Hash algorithm
    password.algorithm = 'bcrypt'
    // Number of bcrypt rounds
    password.bcrypt.logrounds = 14

    providerNames = [
            'daoAuthenticationProvider',
            'anonymousAuthenticationProvider',
            'rememberMeAuthenticationProvider',
    ]

    if (org.transmartproject.app.oauthEnabled) {
        providerNames << 'clientCredentialsAuthenticationProvider'

        def securedResourcesFilters = [
                'JOINED_FILTERS',
                '-securityContextPersistenceFilter',
                '-logoutFilter',
                '-rememberMeAuthenticationFilter',
                '-basicAuthenticationFilter',
                '-exceptionTranslationFilter',
        ].join(',')

        filterChain.chainMap = [
                '/oauth/token': [
                        'JOINED_FILTERS',
                        '-oauth2ProviderFilter',
                        '-securityContextPersistenceFilter',
                        '-logoutFilter',
                        '-rememberMeAuthenticationFilter',
                        '-exceptionTranslationFilter',
                ].join(','),
                '/oauth/inspectToken': securedResourcesFilters,
                '/versions/**': securedResourcesFilters,
                '/v1/**': securedResourcesFilters,
                '/v2/**': securedResourcesFilters,
                '/**': [
                        'JOINED_FILTERS',
                        '-statelessSecurityContextPersistenceFilter',
                        '-oauth2ProviderFilter',
                        '-clientCredentialsTokenEndpointFilter',
                        '-basicAuthenticationFilter',
                        '-oauth2ExceptionTranslationFilter'
                ].join(','),
        ].collect { k, v ->
            [pattern: k, filters: v]
        }

        grails.exceptionresolver.params.exclude = ['password', 'client_secret']
    }

} } }
/* }}} */

/* {{{ GlowingBear query subscription configuration */
grails {
    mail {
        host = "smtp.gmail.com"
        'default' {
            from = "subscription.gb@gmail.com"
        }
        port = 465
        username = "subscription.gb@gmail.com"
        password = "gb@TheHyve"
        props = ["mail.smtp.auth":"true",
                 "mail.smtp.ssl.enable": "true",
                 "mail.smtp.socketFactory.port":"465",
                 "mail.smtp.socketFactory.class":"javax.net.ssl.SSLSocketFactory",
                 "mail.smtp.socketFactory.fallback":"false"]
    }
}
/* }}} */

/* {{{ Query subscription configuration */
//Quartz plugin configuration - job for queryDiff subscription
quartz {
    autoStartup = true
}
// max number of queryDiff results for pagination
org.transmart.server.subscription.numResults = 20
/* }}} */




/* {{{ DATASOURCES configuration */
// OAuth2 dataSource settings are a part of transmart-oauth config

dataSources {
    oauth2 {
        driverClassName = 'org.h2.Driver'
        url = "jdbc:h2:~/.grails/oauth2db;MVCC=TRUE"
        dialect = 'org.hibernate.dialect.H2Dialect'
        username = 'sa'
        password = ''
        dbCreate = 'update'
        logSql = true
        formatSql = true
    }
}
hibernate {
    cache.use_query_cache        = true
    cache.use_second_level_cache = true

    // make sure hibernate.cache.provider_class is not being set
    // see http://stackoverflow.com/a/3690212/127724 and the docs for the cache-ehcache plugin
    //cache.region.factory_class   = 'grails.plugin.cache.ehcache.hibernate.BeanEhcacheRegionFactory'
    cache.region.factory_class = 'org.hibernate.cache.ehcache.SingletonEhCacheRegionFactory'
}

environments {
    test {
        dataSources {
            dataSource {
                driverClassName = 'org.h2.Driver'
                url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;INIT=RUNSCRIPT FROM './h2_init.sql'"
                dialect = 'org.hibernate.dialect.H2Dialect'
                username = 'sa'
                password = ''
                dbCreate = 'update'
                logSql = true
                formatSql = true
            }
            oauth2 {
                driverClassName = 'org.h2.Driver'
                url = "jdbc:h2:mem:oauth2;MVCC=TRUE"
                dialect = 'org.hibernate.dialect.H2Dialect'
                username = 'sa'
                password = ''
                dbCreate = 'update'
                logSql = true
                formatSql = true
            }
        }
        hibernate {
            cache.use_second_level_cache = true
            cache.use_query_cache = false
        }
    }
}
/* }}} */
