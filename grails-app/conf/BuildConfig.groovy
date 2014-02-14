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

grails.project.dependency.resolver = 'maven'
grails.project.dependency.resolution = {
    inherits('global') {
        // specify dependency exclusions here; for example, uncomment this to disable ehcache:
        // excludes 'ehcache'
    }
    log 'warn'

    repositories {
        mavenRepo 'https://repo.thehyve.nl/content/groups/public/'
        inherits false // inherit repository definitions from plugins (default true)
    }

    dependencies {
        compile 'org.transmartproject:transmart-core-api:1.0-SNAPSHOT'
        compile 'org.javassist:javassist:3.16.1-GA'

        compile 'junit:junit:4.11'

        test 'org.gmock:gmock:0.8.3', {
            transitive = false /* don't bring groovy-all */
        }
        test 'org.hamcrest:hamcrest-library:1.3'
        test 'org.hamcrest:hamcrest-core:1.3'

        test 'org.codehaus.groovy.modules.http-builder:http-builder:0.6'
    }

    plugins {
        build   ':tomcat:7.0.47'

        compile ':cache:1.1.1'

        compile ':spring-security-core:2.0-RC2'
        compile ':spring-security-oauth2-provider:1.0.5.1'

        compile ':oauth:2.1.0'

        compile ':transmart-user-management:1.0-SNAPSHOT'

        runtime ':hibernate:3.6.10.6'
        runtime ':jquery:1.10.2.2'

        runtime ':transmart-core:1.0-SNAPSHOT'
    }
}

def buildConfigFile = new File(
        "${userHome}/.grails/transmartConfig/BuildConfig-rest.groovy")
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
