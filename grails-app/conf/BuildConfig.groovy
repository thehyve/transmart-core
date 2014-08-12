grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

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

grails.project.repos.default = 'repo.thehyve.nl-snapshots'
grails.project.repos."${grails.project.repos.default}".url = 'https://repo.thehyve.nl/content/repositories/snapshots/'

grails.project.dependency.resolver = 'maven'
grails.project.dependency.resolution = {
    log "warn"
    legacyResolve false

    inherits('global') {}

    repositories {
        mavenLocal()
        mavenRepo 'https://repo.thehyve.nl/content/repositories/public/'
    }

    dependencies {
        compile('org.transmartproject:transmart-core-api:1.2.1-SNAPSHOT')
        compile group: 'com.google.guava', name: 'guava', version: '14.0.1'

        runtime('org.postgresql:postgresql:9.3-1100-jdbc41') {
            transitive = false
            export     = false
        }

        test('junit:junit:4.11') {
            transitive = false /* don't bring hamcrest */
            export     = false
        }

        test 'org.hamcrest:hamcrest-core:1.3',
             'org.hamcrest:hamcrest-library:1.3'

        test 'org.gmock:gmock:0.9.0-r435-hyve2', {
            transitive = false /* don't bring groovy-all */
        }
    }

    plugins {
        build ':tomcat:7.0.47'
        build ':release:3.0.1', ':rest-client-builder:2.0.1', {
            export = false
        }

        compile(':db-reverse-engineer:0.5') {
            export = false
        }

        runtime ':hibernate:3.6.10.4'

        test ":code-coverage:1.2.6", {
            export = false
        }
    }
}
