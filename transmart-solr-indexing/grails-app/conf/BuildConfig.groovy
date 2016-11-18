grails.project.class.dir = 'target/classes'
grails.project.test.class.dir = 'target/test-classes'
grails.project.test.reports.dir = 'target/test-reports'

grails.project.fork = [
    test:    [maxMemory: 2048, minMemory: 64, debug: false, maxPerm: 768, daemon: true],
    run:     [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, forkReserve: false],
    war:     [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, forkReserve: false],
    console: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256]
]

def dm, dmClass
try {
    dmClass = new GroovyClassLoader().parseClass(
            new File('../../transmart-dev/DependencyManagement.groovy'))
} catch (Exception e) { }
if (dmClass) {
    dm = dmClass.newInstance()
}

grails.project.dependency.resolver = 'maven'
grails.project.dependency.resolution = {
    inherits('global') {}
    log 'warn'
    repositories {
        grailsCentral()
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        compile 'org.apache.solr:solr-solrj:4.5.1'
    }

    plugins {
        compile ':cache-ehcache:1.0.5'
//      runtime ':db-reverse-engineer:3.0.0'

        if (!dm) {
            compile ':folder-management:1.2.2-SNAPSHOT'
            compile ':biomart-domain:1.2.2-SNAPSHOT'
            runtime ':transmart-core:1.2.2-SNAPSHOT'
            test ':transmart-core-db-tests:1.2.2-SNAPSHOT'
        } else {
            dm.internalDependencies delegate
        }
    }
}

dm?.with {
    configureInternalPlugin 'compile', 'folder-management'
    configureInternalPlugin 'compile', 'biomart-domain'
    configureInternalPlugin 'runtime', 'transmart-core'
    configureInternalPlugin 'test', 'transmart-core-db-tests'
}

dm?.inlineInternalDependencies grails, grailsSettings
