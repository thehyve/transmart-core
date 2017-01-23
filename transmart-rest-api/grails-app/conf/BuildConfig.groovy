/*
 * Copyright 2014 Janssen Research & Development, LLC.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version, along with the following terms:
 *
 *   1. You may convey a work based on this program in accordance with
 *      section 5, provided that you retain the above notices.
 *   2. You may convey verbatim copies of this program code as you receive
 *      it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
grails.servlet.version = '3.0'

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

final def CLOVER_VERSION = '4.1.1'
def enableClover = System.getenv('CLOVER')

def dm, dmClass
try {
    dmClass = new GroovyClassLoader().parseClass(
            new File('../transmart-dev/DependencyManagement.groovy'))
} catch (Exception e) {
}
if (dmClass) {
    dm = dmClass.newInstance()
}

if (enableClover) {
    grails.project.fork.test = false

    clover {
        on = true

        srcDirs = ['src/java', 'src/groovy', 'grails-app', 'test']
        excludes = ['**/conf/**', '**/plugins/**', '**/HighDimProtos.java']

        reporttask = { ant, binding, plugin ->
            def reportDir = "${binding.projectTargetDir}/clover/report"
            ant.'clover-report' {
                ant.current(outfile: reportDir, title: 'transmart-rest-api') {
                    format(type: "html", reportStyle: 'adg')
                    testresults(dir: 'target/test-reports', includes: '*.xml')
                    ant.columns {
                        lineCount()
                        filteredElements()
                        uncoveredElements()
                        totalPercentageCovered()
                    }
                }
                ant.current(outfile: "${reportDir}/clover.xml") {
                    format(type: "xml")
                    testresults(dir: 'target/test-reports', includes: '*.xml')
                }
            }
        }
    }
}

grails.project.dependency.resolver = 'maven'
grails.project.dependency.resolution = {
    inherits('global') {
        // specify dependency exclusions here; for example, uncomment this to disable ehcache:
        // excludes 'ehcache'
    }
    log 'warn'

    if (!dm) {
        repositories {
            grailsCentral()
            mavenCentral()

            mavenRepo "https://repo.transmartfoundation.org/content/repositories/public/"
            mavenRepo "https://repo.thehyve.nl/content/repositories/public/"
        }
    } else {
        dm.configureRepositories delegate
    }

    dependencies {
        compile 'org.transmartproject:transmart-core-api:16.2-SNAPSHOT'
        compile 'org.javassist:javassist:3.16.1-GA'

        // includes fix for GRAILS-11126
        compile 'org.grails:grails-plugin-rest:2.3.5-hyve4'

        compile 'com.google.protobuf:protobuf-java:2.5.0'

        runtime 'org.postgresql:postgresql:9.3-1100-jdbc41', {
            export = false
        }
        runtime 'com.oracle:ojdbc7:12.1.0.1', {
            export = false
        }

        test 'org.gmock:gmock:0.8.3', {
            transitive = false /* don't bring groovy-all */
        }
        test 'junit:junit:4.11', {
            transitive = false /* don't bring hamcrest */
        }
        test 'org.hamcrest:hamcrest-library:1.3'
        test 'org.hamcrest:hamcrest-core:1.3'
        test 'org.codehaus.groovy.modules.http-builder:http-builder:0.6', {
            excludes 'groovy', 'nekohtml'
            exported = false
        }
    }

    plugins {
        build ':release:3.0.1', ':rest-client-builder:2.0.1', {
            export = false
        }
        build ':tomcat:7.0.47', {
            export = false
        }

        compile ':spring-security-core:2.0-RC2'

        // core-db doesn't export hibernate as dep as it was builtin in 2.2.4
        runtime ':hibernate:3.6.10.16'

        test ':functional-test:2.0.0'

        if (!dm) {
            runtime ':transmart-core:16.2-SNAPSHOT'

            test ':transmart-core:16.2-SNAPSHOT'
            test ':transmart-core-db-tests:16.2-SNAPSHOT'
        } else {
            dm.internalDependencies delegate
        }

        if (enableClover) {
            compile ":clover:$CLOVER_VERSION", {
                export = false
            }
        }
    }
}

dm?.with {
    configureInternalPlugin 'runtime', 'transmart-core'
    configureInternalPlugin 'test', 'transmart-core'
    configureInternalPlugin 'test', 'transmart-core-db-tests'
}

dm?.inlineInternalDependencies grails, grailsSettings
Z