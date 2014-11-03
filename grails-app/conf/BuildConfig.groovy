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

import grails.util.Environment

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

// prefer to add this to ~/.grails/transmartConfig/BuildConfig-rest-api.groovy
// to avoid committing changes here by accident
//grails.plugin.location.'transmart-core-db' = '../transmart-core-db/'
//grails.plugin.location.'transmart-core-db-tests' = '../transmart-core-db/transmart-core-db-tests/'
//grails.plugin.location.'transmart-core-db-tests' = '../transmart-core-db/transmart-core-db-tests/'

grails.project.dependency.resolver = 'maven'
grails.project.dependency.resolution = {
    inherits('global') {
        // specify dependency exclusions here; for example, uncomment this to disable ehcache:
        // excludes 'ehcache'
    }
    log 'warn'

    repositories {
        mavenLocal()
        mavenRepo 'https://repo.transmartfoundation.org/content/repositories/public/'
        mavenRepo 'https://repo.thehyve.nl/content/groups/public/'
        inherits false // inherit repository definitions from plugins (default true)
    }

    dependencies {
        compile 'org.transmartproject:transmart-core-api:1.2.2-SNAPSHOT'
        compile 'org.javassist:javassist:3.16.1-GA'

        // includes fix for GRAILS-11126
        compile 'org.grails:grails-plugin-rest:2.3.5-hyve4'

        compile 'com.google.protobuf:protobuf-java:2.5.0'

        runtime 'org.postgresql:postgresql:9.3-1100-jdbc41', {
            exported = false
        }
        runtime 'com.oracle:ojdbc6:11.2.0.3.0', {
            exported = false
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
        build ":release:3.0.1"
        build ':tomcat:7.0.47'

        compile ':spring-security-core:2.0-RC2'

        runtime ':transmart-core:1.2.2-SNAPSHOT'
        // core-db doesn't export hibernate as dep as it was builtin in 2.2.4
        runtime ':hibernate:3.6.10.6'

        // tests depend on transmart-core-db-tests which is not part of the release yet
        test ':functional-test:2.0.RC1'
        test ':transmart-core:1.2.2-SNAPSHOT'
        test ':transmart-core-db-tests:1.2.2-SNAPSHOT'
    }
}

def buildConfigFile = new File(
        "${userHome}/.grails/transmartConfig/BuildConfig-rest-api.groovy")
if (buildConfigFile.exists()) {
    println "[INFO] Processing external build config at $buildConfigFile"

    def slurpedBuildConfig = new ConfigSlurper(Environment.current.name).
            parse(buildConfigFile.toURL())

    slurpedBuildConfig.grails.plugin.location.each { String k, v ->
        if (!new File(v).exists()) {
            println "[WARNING] Cannot load in-place plugin from ${v} as that " +
                    "directory does not exist."
        } else {
            println "[INFO] Loading in-place plugin $k from $v"
            grails.plugin.location."$k" = v
        }
        if (grailsSettings.projectPluginsDir?.exists()) {
            grailsSettings.projectPluginsDir.eachDir { dir ->
                // remove optional version from inline definition
                def dirPrefix = k.replaceFirst(/:.+/, '') + '-'
                if (dir.name.startsWith(dirPrefix)) {
                    println "[WARNING] Found a plugin directory at $dir that is a " +
                            "possible conflict and may prevent grails from using " +
                            "the in-place $k plugin."
                }
            }
        }
    }
}
