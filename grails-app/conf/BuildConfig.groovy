grails.servlet.version = '3.0'
grails.plugin.location.'transmart-core' = '../core-db'
grails.plugin.location.'transmart-user-management' = '../transmart-user-management'

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
        inherits true // inherit repository definitions from plugins (default true)
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

        compile ':scaffolding:2.0.1'
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
