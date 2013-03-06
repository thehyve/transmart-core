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
        grailsCentral()
        mavenLocal()
        mavenCentral()
        // uncomment the below to enable remote dependency resolution
        // from public Maven repositories
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.

        compile('org.transmartproject:transmart-core-api:1.0-SNAPSHOT')

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
              ":release:2.2.0",
              ":rest-client-builder:1.0.3",
              ) {
            exported: false
        }
    }
}
