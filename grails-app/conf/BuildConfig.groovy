/*************************************************************************   
* Copyright 2008-2012 Janssen Research & Development, LLC.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
******************************************************************/

def forkSettingsRun = [
        minMemory: 1536,
        maxMemory: 4096,
        maxPerm:   384,
        debug:     false,
]
def forkSettingsOther = [
        minMemory: 256,
        maxMemory: 1024,
        maxPerm:   384,
        debug:     false,
]

grails.project.fork = [
        test:    [ *:forkSettingsOther, daemon: true ],
        run:     forkSettingsRun,
        war:     forkSettingsRun,
        console: forkSettingsOther ]

grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.dependency.resolver = "maven"
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn"

    repositories {
        // grailsPlugins()
        // grailsHome()
        grailsCentral()

        mavenLocal()
        mavenCentral()
        mavenRepo 'https://repo.transmartfoundation.org/content/repositories/public/'
    }
    dependencies {
        compile 'net.sf.opencsv:opencsv:2.3'
        compile 'org.rosuda:Rserve:1.7.3'
        compile 'org.mapdb:mapdb:0.9.10'

        /* serializable ImmutableMap only on guava 16 */
        compile group: 'com.google.guava', name: 'guava', version: '16.0-dev-20140115-68c8348'
        compile 'org.transmartproject:transmart-core-api:16.1'

        /* compile instead of test due to technical limitations
         * (referenced from resources.groovy) */
        runtime 'org.gmock:gmock:0.8.3', {
            transitive = false /* don't bring groovy-all */
            export     = false
        }
        test('org.hamcrest:hamcrest-library:1.3',
                'org.hamcrest:hamcrest-core:1.3') {
            export     = false
        }
    }

    plugins {
        build(':release:3.0.1',
              ':rest-client-builder:1.0.3') { export = false }

        compile ':sendfile:0.2'
        compile ':quartz:1.0-RC2'

        runtime ':resources:1.2.1'

        // support for static code analysis
        compile ":codenarc:0.21"
    }
}

codenarc.reports = {
    TransmartAppReport('html') {
        outputFile = 'CodeNarc-Rmodules-Report.html'
        title = 'Rmodules Report'
    }
}
