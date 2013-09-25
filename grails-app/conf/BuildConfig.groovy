grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.repos.default = 'repo.thehyve.nl-snapshots'
grails.project.repos."${grails.project.repos.default}".url = 'http://repo.thehyve.nl/content/repositories/snapshots/'
grails.project.repos."${grails.project.repos.default}".type = "maven"
grails.project.repos."${grails.project.repos.default}".username = "deployment"
grails.project.repos."${grails.project.repos.default}".password = "7FizAgzECt9k"
//grails.project.repos."${grails.project.repos.default}".portal = "grailsCentral"

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
        mavenCentral()
        // uncomment the below to enable remote dependency resolution
        // from public Maven repositories
        //mavenLocal()
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.

        // runtime 'mysql:mysql-connector-java:5.1.21'
    }

    plugins {
        runtime ':resources:1.2'
        build(":tomcat:7.0.41",
              ":release:2.2.1",
              ":rest-client-builder:1.0.3") {
            export = false
        }
    }
}
