grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.repos.default = 'repo.thehyve.nl-snapshots'
grails.project.repos."${grails.project.repos.default}".url = 'https://repo.thehyve.nl/content/repositories/snapshots/'

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    legacyResolve false // whether to do a secondary resolve on plugin installation, not advised and here for backwards compatibility
    repositories {
        grailsCentral()
        mavenLocal()
        mavenCentral()
        mavenRepo 'https://repo.thehyve.nl/content/repositories/public/'
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.

        compile('org.transmartproject:transmart-core-api:1.0-SNAPSHOT')
        compile group: 'com.google.guava', name: 'guava', version: '14.0.1'

        runtime('org.postgresql:postgresql:9.3-1100-jdbc41') {
            transitive = false
            export     = false
        }

        /* for unknown reason, test scope is not enough */
        compile('junit:junit:4.11') {
            transitive = false /* don't bring hamcrest */
            export     = false
        }

        compile('com.h2database:h2:1.3.174') {
            export = false
        }

        test('org.hamcrest:hamcrest-library:1.3',
             'org.hamcrest:hamcrest-core:1.3') {
            export     = false
        }

        test('org.gmock:gmock:0.8.3') {
            transitive = false /* don't bring groovy-all */
            export     = false
        }

        /* for reasons I don't want to guess (we'll move away from ivy soon
         * anyway), javassist is not being included in the test classpath
         * when running test-app in Travis even though the hibernate plugin
         * depends on it */
        test('org.javassist:javassist:3.16.1-GA')
    }

    plugins {
        compile(':db-reverse-engineer:0.5') {
            export = false
        }

        build(':tomcat:2.2.4',
              ":release:2.2.1",
              ":rest-client-builder:1.0.3",
              ) {
            export = false
        }

        test ":code-coverage:1.2.6", {
            export = false
        }
    }
}
