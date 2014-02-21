grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.repos.default = 'repo.thehyve.nl-snapshots'
grails.project.repos."${grails.project.repos.default}".url = 'https://repo.thehyve.nl/content/repositories/snapshots/'
grails.plugin.location.'transmart-core-db' = '../.'

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
        grailsCentral()
        mavenLocal()
        mavenCentral()
        mavenRepo 'https://repo.thehyve.nl/content/repositories/public/'
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.
        // runtime 'mysql:mysql-connector-java:5.1.24'
        /* for unknown reason, test scope is not enough */
        compile('junit:junit:4.11') {
            transitive = false /* don't bring hamcrest */
            export     = false
        }

        test('org.hamcrest:hamcrest-library:1.3',
                'org.hamcrest:hamcrest-core:1.3') {
            export     = false
        }

        test('org.gmock:gmock:0.8.3') {
            transitive = false /* don't bring groovy-all */
            export     = false
        }
    }

    plugins {
        build(":release:2.2.1",
              ":rest-client-builder:1.0.3") {
            export = false
        }
    }
}
