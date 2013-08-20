grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"


grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    legacyResolve false // whether to do a secondary resolve on plugin installation, not advised and here for backwards compatibility
    repositories {
        mavenRepo([
                name: 'repo.transmartfoundation.org',
                root: 'http://repo.transmartfoundation.org/content/repositories/public/'
        ])
        grailsCentral()
        mavenLocal()
        mavenCentral()

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
        compile(':db-reverse-engineer:0.5') { exported: false }

        build(":tomcat:$grailsVersion",
              ":release:2.2.1",
              ":rest-client-builder:1.0.3",
              ) {
            exported: false
        }

		test ":code-coverage:1.2.6"
    }

    // see http://jira.grails.org/browse/GPRELEASE-42
    if (grailsVersion >= '2.1.3')
        legacyResolve true
}
