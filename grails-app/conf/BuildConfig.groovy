grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.repos.default = 'repo.thehyve.nl-snapshots'
grails.project.repos."${grails.project.repos.default}".url = 'http://repo.thehyve.nl/content/repositories/snapshots/'

grails.project.dependency.resolver = 'maven'

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn"
    repositories {
        grailsCentral()
        mavenCentral()

        mavenRepo([
                name: 'repo.thehyve.nl-snapshots',
                url: 'http://repo.thehyve.nl/content/repositories/snapshots/',
        ])
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.

        compile('org.transmartproject:transmart-core-api:1.0-SNAPSHOT')
        compile group: 'com.google.guava', name: 'guava', version: '14.0.1'

        runtime('postgresql:postgresql:9.1-901.jdbc4') {
            transitive: false
        }

        /* for unknown reason, test scope is not enough */
        compile('junit:junit:4.11') {
            transitive: false
        }

        test('org.hamcrest:hamcrest-library:1.3',
             'org.hamcrest:hamcrest-core:1.3')
    }

    plugins {
        compile ':hibernate:3.6.10.1'
        compile(':db-reverse-engineer:0.5') { exported: false }

        build(":tomcat:7.0.42",
              ":release:3.0.1",
              ":rest-client-builder:1.0.3",
              ) {
            exported: false
        }

		test ":code-coverage:1.2.6"
    }
}
