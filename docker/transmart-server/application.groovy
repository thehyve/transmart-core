dataSources {
    dataSource {
        driverClassName = 'org.postgresql.Driver'
        url = 'jdbc:postgresql://' + System.getenv('DB_HOST') + ':' + System.getenv('DB_PORT') + '/' + System.getenv('DB_NAME')
        dialect = 'org.hibernate.dialect.PostgreSQLDialect'
//        username = 'biomart_user'
//        password = 'biomart_user'
//        dbCreate = 'none'
//        driverClassName = 'oracle.jdbc.driver.OracleDriver'
//        url = 'jdbc:oracle:thin:@localhost:1521:ORCL'
//        dialect = 'org.hibernate.dialect.Oracle10gDialect'
        username = System.getenv('DB_USER')
        password = System.getenv('DB_PW')
        dbCreate = 'none'
        logSql = true
        formatSql = true
    }
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

environments {
    development {
        dataSource {
            logSql    = true
            formatSql = true
            properties {
                maxActive   = 10
                maxIdle     = 5
                minIdle     = 2
                initialSize = 2
            }
        }
    }
    production {
        dataSource {
            logSql    = false
            formatSql = false
            properties {
                maxActive   = 50
                maxIdle     = 25
                minIdle     = 5
                initialSize = 5
            }
        }
    }
}


/*
 * NOTE
 * ----
 * This configuration assumes that the development environment will be used with
 * run-app and the production environment will be used with the application
 * packaged as a WAR and deployed to tomcat. Running grails run-war or otherwise
 * running a WAR with the development profile set up or activating the
 * production environment when running grails run-app are scenarios that have
 * NOT been tested.
 */


// if running as a WAR, we need these
// def explodedWarDir    = catalinaBase + '/deployments/transmart-17.1'
def solrHost          = System.getenv('SOLR_HOST') //host of appserver where solr runs
def solrPort          = System.getenv('SOLR_PORT') //port of appserver where solr runs (under ctx path /solr)
def searchIndex       = System.getProperty("user.home") + '/searchIndex' //create this directory
// for running transmart as WAR, create this directory and then create an alias
def jobsDirectory     = "/var/tmp/jobs/"
def samlEnabled  = false
org.transmartproject.app.oauthEnabled = true
org.transmartproject.app.gwavaEnabled = false
org.transmartproject.app.transmartURL = System.getenv('TRANSMART_URL')

//Disabling/Enabling UI tabs
ui {
    tabs {
        //Search was not part of 1.2. It's not working properly. You need to set `show` to `true` to see it on UI
        search.show = false
        browse.hide = false
        //Note: analyze tab is always shown
        sampleExplorer.hide = false
        geneSignature.hide = false
        gwas.hide = false
        uploadData.hide = false
        datasetExplorer {
            gridView.hide = false
            dataExport.hide = false
            dataExportJobs.hide = false
            // Note: by default the analysisJobs panel is NOT shown
            // Currently, it is only used in special cases
            analysisJobs.show = false
            workspace.hide = false
        }
    }
    /*
    //The below disclaimer appears on the login screen, just below the login button.
    loginScreen {
        disclaimer = "Please be aware that tranSMART is a data-integration tool that allows for exploration of available study data. The information shown in tranSMART, and derived from performed analyses, are for research purposes only. NOT for decision making in e.g. clinical trial studies."
    }
    */
}

// I001 â€“ Insertion point 'post-WAR-variables'

//transmartURL      = 'http://example.com/transmart/'
//oauthEnabled      = true
//samlEnabled       = false
//gwavaEnabled      = false

/* Other things you may want to change:
 * â€“ Log4j configuration
 * â€“ 'Personalization & login' section
 * â€“ Location of Solr instance in 'Faceted Search Configuration'
 * â€“ For enabling SAML, editing the corresponding section is mandatory
 */

/* If you want to be able to regenerate this file easily, instead of editing
 * the generated file directly, create a Config-extra.groovy file in the root of
 * the transmart-data checkout. That file will be appended to this one whenever
 * the Config.groovy target is run */

environments { production {
    if (org.transmartproject.app.transmartURL.startsWith('http://localhost:')) {
        println "[WARN] transmartURL not overridden. Some settings (e.g. help page) may be wrong"
    }
} }

/* {{{ Faceted Search Configuration */
com.rwg.solr.scheme = 'http'
com.rwg.solr.host   = solrHost + ':' + solrPort
com.rwg.solr.path   = '/solr/rwg/select/'
/* }}} */

/* {{{ Data Upload Configuration - see GWAS plugin Data Upload page */
// This is the value that will appear in the To: entry of the e-mail popup
// that is displayed when the user clicks the Email administrator button,
// on the GWAS plugin Data Upload page
com.recomdata.dataUpload.adminEmail = 'No data upload adminEmail value set - contact site administrator'
/* }}} */

/* {{{ Personalization */
// application logo to be used in the login page
com.recomdata.largeLogo = "transmartlogo.jpg"

// application logo to be used in the search page
com.recomdata.smallLogo="transmartlogosmall.jpg"

// contact email address
com.recomdata.contactUs = "transmart-discuss@googlegroups.com"

// site administrator contact email address
com.recomdata.adminEmail = "transmart-discuss@googlegroups.com"

// application title
com.recomdata.appTitle = "tranSMART v" + grails.util.Metadata.current.getApplicationVersion()

// Location of the help pages. Should be an absolute URL.
// Currently, these are distribution with transmart,
// so it can also point to that location copy.
com.recomdata.adminHelpURL = "${org.transmartproject.app.transmartURL}/help/adminHelp/default.htm"

environments { development {
    com.recomdata.bugreportURL = 'https://jira.transmartfoundation.org'
} }

// Keys without defaults (see Config-extra.php.sample):
// com.recomdata.projectName
// com.recomdata.providerName
// com.recomdata.providerURL
/* }}} */

/* {{{ Login */
// Session timeout and heartbeat frequency (ping interval)
com.recomdata.sessionTimeout = 1800
com.recomdata.heartbeatLaps = 300

environments { development {
    com.recomdata.sessionTimeout = Integer.MAX_VALUE / 1000 as int /* ~24 days */
    com.recomdata.heartbeatLaps = 900
} }

// Maximum concurrent sessions for a user (-1: unlimited)
// org.transmartproject.maxConcurrentUserSessions = 10

// Not enabled by default (see Config-extra.php.sample)
//com.recomdata.passwordstrength.pattern
//com.recomdata.passwordstrength.description

// Whether to enable guest auto login.
// If it's enabled no login is required to access tranSMART.
com.recomdata.guestAutoLogin = false
environments { development { com.recomdata.guestAutoLogin = false } }

// Guest account user name - if guestAutoLogin is true, this is the username of
// the account that tranSMART will automatically authenticate users as. This will
// control the level of access anonymous users will have (the access will match
// that of the account specified here).
com.recomdata.guestUserName = 'guest'
/* }}} */

/* {{{ Search tool configuration */

// Lucene index location for documentation search
com.recomdata.searchengine.index = searchIndex

/* }}} */

/* {{{ Sample Explorer configuration */

// This is an object to dictate the names and 'pretty names' of the SOLR fields.
// Optionally you can set the width of each of the columns when rendered.

sampleExplorer {
    fieldMapping = [
            columns:[
                    [header:'Sample ID',dataIndex:'id', mainTerm: false, showInGrid: false],
                    [header:'BioBank', dataIndex:'BioBank', mainTerm: true, showInGrid: true, width:10],
                    [header:'Source Organism', dataIndex:'Source_Organism', mainTerm: true, showInGrid: true, width:10]
                    // Continue as you have fields
            ]
    ]
    resultsGridHeight = 100
    resultsGridWidth = 100
    idfield = 'id'
}

edu.harvard.transmart.sampleBreakdownMap = [
        "id":"Aliquots in Cohort"
]

// Solr configuration for the Sample Explorer
com { recomdata { solr {
    maxNewsStories = 10
    maxRows = 10000
}}}

/* }}} */

/* {{{ Dataset Explorer configuration */
com { recomdata { datasetExplorer {
    // set to 'true' (quotes included) to enable gene pattern integration
    genePatternEnabled = 'false'
    // The tomcat URL that gene pattern is deployed within -usually it's proxyed through apache
    genePatternURL = 'http://23.23.185.167'
    // Gene Pattern real URL with port number
    genePatternRealURLBehindProxy = 'http://23.23.185.167:8080'
    // default Gene pattern user to start up - each tranSMART user will need a separate user account to be created in Gene Pattern
    genePatternUser = 'biomart'

    // Absolute path to PLINK executables
    plinkExcutable = '/usr/local/bin/plink'
} } }
// Metadata view
com.recomdata.view.studyview = 'studydetail'

com.recomdata.plugins.resultSize = 5000
/* }}} */

/* {{{ RModules & Data Export Configuration */
environments {
    // This is to target a remove Rserv. Bear in mind the need for shared network storage
    RModules.host = "127.0.0.1"
    RModules.port = 6311

    // This is not used in recent versions; the URL is always /analysisFiles/
    RModules.imageURL = "/tempImages/" //must end and start with /

    production {
        // The working directory for R scripts, where the jobs get created and
        // output files get generated
        RModules.tempFolderDirectory = "/tmp"
    }
    development {
        RModules.tempFolderDirectory = "/tmp"

        /* we don't need to specify temporaryImageDirectory, because we're not copying */
    }

    // Used to access R jobs parent directory outside RModules (e.g. data export)
    com.recomdata.plugins.tempFolderDirectory = RModules.tempFolderDirectory
}
/* }}} */

/* {{{ GWAS Configuration */

com.recomdata.dataUpload.appTitle="Upload data to tranSMART"
com.recomdata.dataUpload.stageScript="run_analysis_stage"

// Directory path of com.recomdata.dataUpload.stageScript
def gwasEtlDirectory = new File(System.getProperty("user.home"), '.grails/transmart-gwasetl')

// Directory to hold GWAS file uploads
def gwasUploadsDirectory = new File(System.getProperty("user.home"), '.grails/transmart-datauploads')

// Directory to preload with template files with names <type>-template.txt
def gwasTemplatesDirectory = new File(System.getProperty("user.home"), '.grails/transmart-templates')

com.recomdata.dataUpload.templates.dir = gwasTemplatesDirectory.absolutePath
com.recomdata.dataUpload.uploads.dir = gwasUploadsDirectory.absolutePath
com.recomdata.dataUpload.etl.dir = gwasEtlDirectory.absolutePath

[gwasTemplatesDirectory, gwasUploadsDirectory, gwasEtlDirectory].each {
    if (!it.exists()) {
        it.mkdir()
    }
}

/* }}} */

/* {{{ Misc Configuration */

// This can be used to debug JavaScript callbacks in the dataset explorer in
// Chrome. Unfortunately, it also sometimes causes chrome to segfault
com.recomdata.debug.jsCallbacks = 'false'

environments {
    production {
        com.recomdata.debug.jsCallbacks = 'false'
    }
}

grails.resources.adhoc.excludes = [ '/images' + RModules.imageURL + '**' ]

// Adding properties to the Build information panel
buildInfo { properties {
    include = [ 'app.grails.version', 'build.groovy' ]
    exclude = [ 'env.proc.cores' ]
} }

/* }}} */

/* {{{ Spring Security configuration */

grails.cors.enabled = true

grails { plugin { springsecurity {

    if (org.transmartproject.app.oauthEnabled) {

        def glowingBearRedirectUris = [
                org.transmartproject.app.transmartURL - ~/transmart\/?$/ + '/connections',
        ]
        // for dev, node reverse proxy runs on 8001
        glowingBearRedirectUris << 'http://localhost:8001/connections'
        glowingBearRedirectUris << 'http://localhost'
        glowingBearRedirectUris << 'http://localhost:4200'

        oauthProvider {
            authorization.requireRegisteredRedirectUri = true
            authorization.requireScope = false

            clients = [
                    [
                            clientId: 'api-client',
                            clientSecret: 'api-client',
                            authorities: ['ROLE_CLIENT'],
                            scopes: ['read', 'write'],
                            authorizedGrantTypes: ['authorization_code', 'refresh_token'],
                            redirectUris: [
                                    (org.transmartproject.app.transmartURL - ~'\\/$') + '/oauth/verify',
                                    (org.transmartproject.app.transmartURL - ~'\\/$') + '/v1/oauth/verify'
                            ],
                    ],
                    [
                            clientId: 'glowingbear-js',
                            clientSecret: '',
                            authorities: ['ROLE_CLIENT'],
                            scopes: ['read', 'write'],
                            authorizedGrantTypes: ['implicit', 'password', 'authorization_code', 'refresh_token'],
                            redirectUris: glowingBearRedirectUris,
                    ],
            ]
        }
    }

} } }
/* }}} */

//{{{ SAML Configuration

if (samlEnabled) {
    // don't do assignment to grails.plugin.springsecurity.providerNames
    // see GRAILS-11730
    grails { plugin { springsecurity {
        providerNames << 'samlAuthenticationProvider'
    } } }
    // again, because of GRAILS-11730
    def ourPluginConfig
    grails {
        ourPluginConfig = plugin
    }

    org { transmart { security {
        setProperty('samlEnabled', true) // clashes with local variable
        ssoEnabled  = "true"

        // URL to redirect to after successful authentication
        successRedirectHandler.defaultTargetUrl = ourPluginConfig.springsecurity.successHandler.defaultTargetUrl
        // URL to redirect to after successful logout
        successLogoutHandler.defaultTargetUrl = ourPluginConfig.springsecurity.logout.afterLogoutUrl

        saml {
            /* {{{ Service provider details (we) */
            sp {
                // ID of the Service Provider
                id = "gustavo-transmart"

                // URL of the service provider. This should be autodected, but it isn't
                url = "http://localhost:8080/transmart-17.1"

                // Alias of the Service Provider
                alias = "transmart"

                // Alias of the Service Provider's signing key, see keystore details
                signingKeyAlias = "saml-signing"
                // Alias of the Service Provider's encryption key
                encryptionKeyAlias = "saml-encryption"
            }
            /* }}} */

            // Metadata file of the provider. We insist on keeping instead of just
            // retrieving it from the provider on startup to prevent transmart from
            // being unable to start due to provider being down. A copy will still be
            // periodically fetched from the provider
            idp.metadataFile = '/home/glopes/idp-local-metadata.xml'

            /* {{{ Keystore details */
            keystore {
                // Generate with:
                //  keytool -genkey -keyalg RSA -alias saml-{signing,encryption} \
                //    -keystore transmart.jks -storepass changeit \
                //    -validity 3602 -keysize 2048
                // Location of the keystore. You can use other schemes, like classpath:resource/samlKeystore.jks
                file = 'file:///home/glopes/transmart.jks'

                // keystore's storepass
                password="changeit"

                // keystore's default key
                defaultKey="saml-signing"

                // Alias of the encryption key in the keystore
                encryptionKey.alias="saml-encryption"
                // Password of the key with above alias in the keystore
                encryptionKey.password="changeit"

                // Alias of the signing key in the keystore
                signingKey.alias="saml-signing"
                // Password of the key with above alias in the keystore
                signingKey.password="changeit"
            }
            /* }}} */

            /* {{{ Creation of new users */
            createInexistentUsers = "true"
            attribute.username    = "urn:custodix:ciam:1.0:principal:username"
            attribute.firstName   = "urn:oid:2.5.4.42"
            attribute.lastName    = "urn:oid:2.5.4.4"
            attribute.email       = ""
            attribute.federatedId = "personPrincipalName"
            /* }}} */

            //
            // Except maybe for the binding, you probably won't want to change the rest:
            //

            // Suffix of the login filter, saml authentication is initiated when user browses to this url
            entryPoint.filterProcesses = "/saml/login"
            // SAML Binding to be used for above entry point url.
            entryPoint.binding = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
            // This property must be set otherwise the default binding is used which, in this configuration, is HTTP-ARTIFACT
            entryPoint.defaultAssertionConsumerIndex = "1"

            // Suffix of the Service Provider's metadata, this url needs to be configured on IDP
            metadata.filterSuffix = "/saml/metadata"
            // Id of the spring security's authentication manager
            authenticationManager = "authenticationManager"
            // Whether sessions should be invalidated after logout
            logout.invalidateHttpSession = "true"
            // Id of the spring security user service that should be called to fetch users.
            saml.userService = "org.transmart.FederatedUserDetailsService"
        }
    } } }
} else { // if (!samlEnabled)
    org { transmart { security {
        setProperty('samlEnabled', false) // clashes with local variable
    } } }
}
/* }}} */

/* {{{ gwava */
if (org.transmartproject.app.gwavaEnabled) {
    // assume deployment alongside transmart
    com { recomdata { rwg { webstart {
        def url       = new URL(org.transmartproject.app.transmartURL)
        codebase      = "$url.protocol://$url.host${url.port != -1 ? ":$url.port" : ''}/gwava"
        jar           = './ManhattanViz2.1g.jar'
        mainClass     = 'com.pfizer.mrbt.genomics.Driver'
        gwavaInstance = 'transmartstg'
        transmart.url = org.transmartproject.app.transmartURL - ~'\\/$'
    } } } }
    com { recomdata { rwg { qqplots {
        cacheImages = new File(jobsDirectory, 'cachedQQplotImages').toString()
    } } } }
}
/* }}} */

/* {{{ Quartz jobs configuration */
// start delay for the sweep job
com.recomdata.export.jobs.sweep.startDelay =60000 // d*h*m*s*1000
// repeat interval for the sweep job
com.recomdata.export.jobs.sweep.repeatInterval = 86400000 // d*h*m*s*1000
// specify the age of files to be deleted (in days)
com.recomdata.export.jobs.sweep.fileAge = 3
/* }}} */

/* {{{ File store and indexing configuration */
com.rwg.solr.browse.path   = '/solr/browse/select/'
com.rwg.solr.update.path = '/solr/browse/dataimport/'
com.recomdata.solr.baseURL = "${com.rwg.solr.scheme}://${com.rwg.solr.host}" +
        "${new File(com.rwg.solr.browse.path).parent}"

def fileStoreDirectory = new File(System.getProperty("user.home"), '.grails/transmart-filestore')
def fileImportDirectory = new File(System.getProperty("java.io.tmpdir"), 'transmart-fileimport')
com.recomdata.FmFolderService.filestoreDirectory = fileStoreDirectory.absolutePath
com.recomdata.FmFolderService.importDirectory = fileImportDirectory.absolutePath

[fileStoreDirectory, fileImportDirectory].each {
    if (!it.exists()) {
        it.mkdir()
    }
}
/* }}} */

/* {{{ Sample Explorer configuration */

sampleExplorer {
    fieldMapping = [
            columns:[
                    [header:'ID', dataIndex:'id', mainTerm: true, showInGrid: true, width:20],
                    [header:'trial name', dataIndex:'trial_name', mainTerm: true, showInGrid: true, width:20],
                    [header:'barcode', dataIndex:'barcode', mainTerm: true, showInGrid: true, width:20],
                    [header:'plate id', dataIndex:'plate_id', mainTerm: true, showInGrid: true, width:20],
                    [header:'patient id', dataIndex:'patient_id', mainTerm: true, showInGrid: true, width:20],
                    [header:'external id', dataIndex:'external_id', mainTerm: true, showInGrid: true, width:20],
                    [header:'aliquot id', dataIndex:'aliquot_id', mainTerm: true, showInGrid: true, width:20],
                    [header:'visit', dataIndex:'visit', mainTerm: true, showInGrid: true, width:20],
                    [header:'sample type', dataIndex:'sample_type', mainTerm: true, showInGrid: true, width:20],
                    [header:'description', dataIndex:'description', mainTerm: true, showInGrid: true, width:20],
                    [header:'comment', dataIndex:'comment', mainTerm: true, showInGrid: true, width:20],
                    [header:'location', dataIndex:'location', mainTerm: true, showInGrid: true, width:20],
                    [header:'organism', dataIndex:'source_organism', mainTerm: true, showInGrid: true, width:20]
            ]
    ]
    resultsGridHeight = 100
    resultsGridWidth = 100
    idfield = 'id'
}

edu.harvard.transmart.sampleBreakdownMap = [
        "aliquot_id":"Aliquots in Cohort"
]

com { recomdata { solr {
    maxNewsStories = 10
    maxRows = 10000
}}}



/* }}} */

// I002 â€“ Insertion point 'end'


// {{{ Personalization
// Project name shown on the welcome page
//com.recomdata.projectName = "MyProject"

// name and URL of the supporter entity shown on the welcome page
//com.recomdata.providerName = "tranSMART Foundation"
//com.recomdata.providerURL = "http://www.transmartfoundation.org"

// Contact e-mail
//com.recomdata.contactUs = "support@mycompany.com"
//com.recomdata.adminEmail = "support@mycompany.com"
// }}}

// Password strength criteria, please change description accordingly
//com.recomdata.passwordstrength.pattern = ~/^.*(?=.{8,})(?=.*[a-z])(?=.*[A-Z])(?=.*[\d])(?=.*[\W]).*$/

// Password strength regex that is used to test user's passwords that are entered by themselves.
// Take care to change org.transmartproject.app.user.ChangePasswordCommand.newPassword.lowPasswordStrength
// variable inside messages.properties file of web app.
/*
user.password.strength.regex = '''(?x)
^
(?=.*[A-Z])      #Ensure string has an uppercase letter.
(?=.*[!@\#$&*]) #Ensure string has one special case letter.
(?=.*[0-9])      #Ensure string has a digit.
.{8,}            #Ensure string length is not less then 8.
$'''
*/

//Alternative color scheme for aCGH BED tracks.
//Current colors are captured from cghCall R package.
//Although do not correspond exactly.
/*
dataExport {
    bed {
        acgh {
            rgbColorScheme {
                //white
                invalid       = [255, 255, 255]
                //red
                loss          = [205,   0,   0]
                //dark
                normal        = [ 10,  10,  10]
                //green
                gain          = [  0, 255,   0]
                //dark green
                amplification = [  0, 100,   0]
            }
        }
    }
}
*/

// Password strength description, please change according to pattern
//com.recomdata.passwordstrength.description =
//    'It should contain a minimum of 8 characters including at least ' +
//    '1 upper and 1 lower case letter, 1 digit and 1 special character.'

// You MUST leave this at the end
// Do not move it up, otherwise syntax errors may not be detected

org.transmart.configFine = true

// vim: set fdm=marker et ts=4 sw=4 filetype=groovy ai:
