grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
//grails.project.war.file = "target/${appName}-${appVersion}.war"
grails.project.dependency.resolver = "maven"
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    legacyResolve false
    repositories {
        grailsCentral()
        mavenCentral()
        mavenLocal() // Note: use 'grails maven-install' to install required plugins locally

        mavenRepo "https://repo.transmartfoundation.org/content/repositories/public/"
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.
        // runtime 'mysql:mysql-connector-java:5.1.13'
    }
    plugins {
        compile(':transmart-java:16.2-SNAPSHOT')
        compile(':hibernate:3.6.10.10')
        build(":release:3.0.1",
              ":rest-client-builder:1.0.3") {
            export = false
        }
    }

}
