import grails.util.Environment

grails.servlet.version = '3.0'

def defaultVMSettings = [
        maxMemory: 768,
        minMemory: 64,
        debug:     false,
        maxPerm:   256
]

grails.project.fork = [
    test:    [*: defaultVMSettings, daemon:      true],
    run:     [*: defaultVMSettings, forkReserve: false],
    war:     [*: defaultVMSettings, forkReserve: false],
    console: defaultVMSettings
]

// prefer to add this to ~/.grails/transmartConfig/BuildConfig-rest-api.groovy
// to avoid committing changes here by accident
//grails.plugin.location.'transmart-core-db' = '../transmart-core-db/'
//grails.plugin.location.'transmart-core-db-tests' = '../transmart-core-db/transmart-core-db-tests/'

grails.project.dependency.resolver = 'maven'
grails.project.dependency.resolution = {
    inherits('global') {
        // specify dependency exclusions here; for example, uncomment this to disable ehcache:
        // excludes 'ehcache'
    }
    log 'warn'

    repositories {
        mavenLocal()
        mavenRepo 'https://repo.thehyve.nl/content/groups/public/'
        inherits false // inherit repository definitions from plugins (default true)
    }

    dependencies {
        compile 'org.transmartproject:transmart-core-api:1.0-SNAPSHOT'
        compile 'org.javassist:javassist:3.16.1-GA'

        // includes fix for GRAILS-11126
        compile 'org.grails:grails-plugin-rest:2.3.5-hyve3'

        runtime 'org.postgresql:postgresql:9.3-1100-jdbc41', {
            exported = false
        }
        runtime 'com.oracle:ojdbc6:11.2.0.3.0', {
            exported = false
        }

        // strangely needed...
        runtime 'org.springframework:spring-test:3.2.6.RELEASE'

        test 'org.gmock:gmock:0.8.3', {
            transitive = false /* don't bring groovy-all */
        }
        test 'junit:junit:4.11', {
            transitive = false /* don't bring hamcrest */
        }
        test 'org.hamcrest:hamcrest-library:1.3'
        test 'org.hamcrest:hamcrest-core:1.3'
        test 'org.codehaus.groovy.modules.http-builder:http-builder:0.6'
    }

    plugins {
        build ':tomcat:7.0.47'

        runtime ':transmart-core:1.0-SNAPSHOT'
        // core-db doesn't export hibernate as dep as it was builtin in 2.2.4
        runtime ':hibernate:3.6.10.6'

        test ':functional-test:2.0.RC1'
        test ':transmart-core-db-tests:1.0-SNAPSHOT'
    }
}

def buildConfigFile = new File(
        "${userHome}/.grails/transmartConfig/BuildConfig-rest-api.groovy")
if (buildConfigFile.exists()) {
    println "[INFO] Processing external build config at $buildConfigFile"

    def slurpedBuildConfig = new ConfigSlurper(Environment.current.name).
            parse(buildConfigFile.toURL())

    slurpedBuildConfig.grails.plugin.location.each { String k, v ->
        if (!new File(v).exists()) {
            println "[WARNING] Cannot load in-place plugin from ${v} as that " +
                    "directory does not exist."
        } else {
            println "[INFO] Loading in-place plugin $k from $v"
            grails.plugin.location."$k" = v
        }
        if (grailsSettings.projectPluginsDir?.exists()) {
            grailsSettings.projectPluginsDir.eachDir { dir ->
                // remove optional version from inline definition
                def dirPrefix = k.replaceFirst(/:.+/, '') + '-'
                if (dir.name.startsWith(dirPrefix)) {
                    println "[WARNING] Found a plugin directory at $dir that is a " +
                            "possible conflict and may prevent grails from using " +
                            "the in-place $k plugin."
                }
            }
        }
    }
}
