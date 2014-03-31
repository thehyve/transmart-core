grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
//grails.project.war.file = "target/${appName}-${appVersion}.war"
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
             grailsCentral()
        mavenCentral()
		mavenRepo([
			name: 'repo.transmartfoundation.org-public',
			root: 'https://repo.transmartfoundation.org/content/repositories/public/'
	])
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.
		
		

        // runtime 'mysql:mysql-connector-java:5.1.13'
        runtime 'hsqldb:hsqldb:1.8.0.10'
    }
	
	plugins {
		compile ":resources:1.2.RC2"
		compile(':transmart-java:1.0-SNAPSHOT')
		compile(':biomart-domain:1.0-SNAPSHOT')
		compile(':search-domain:1.0-SNAPSHOT')
		compile (':transmart-legacy-db:0.3-SNAPSHOT')
		compile (':spring-security-core:2.0-RC2')
		//compile ':spring-security-ldap:2.0-RC2')
		compile (':quartz:1.0-RC2')
		build(":release:2.2.1",
			":rest-client-builder:1.0.3") {
		export = false
	}
		
	}
}
