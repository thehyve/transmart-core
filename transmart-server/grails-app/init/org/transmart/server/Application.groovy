package org.transmart.server

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import grails.transaction.Transactional
import groovy.util.logging.Slf4j
import org.grails.config.NavigableMap
import org.grails.config.NavigableMapPropertySource
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.security.SecurityFilterAutoConfiguration
import org.springframework.context.EnvironmentAware
import org.springframework.core.env.Environment

@Slf4j
@EnableAutoConfiguration(exclude = [SecurityFilterAutoConfiguration])
class Application extends GrailsAutoConfiguration implements EnvironmentAware {
    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }
    @Override
    void setEnvironment(Environment environment) {
        if (grails.util.Environment.current == grails.util.Environment.TEST) {
            return
        }

        String applicationName = 'transmart'
        String userHome = System.getProperty('user.home')

        addToEnvironmentAsFirstSource(environment, "${userHome}/.grails/${applicationName}Config/Config.groovy")
        addToEnvironmentAsFirstSource(environment, "${userHome}/.grails/${applicationName}Config/DataSource.groovy")
        addToEnvironmentAsFirstSource(environment, "${userHome}/.grails/${applicationName}Config/application.groovy")

        def externalConfig = System.getenv("${applicationName.toUpperCase(Locale.ENGLISH)}_CONFIG_LOCATION")
        if (externalConfig) {
            addToEnvironmentAsFirstSource(environment, externalConfig)
        }
        def externalDataSource = System.getenv("${applicationName.toUpperCase(Locale.ENGLISH)}_DATASOURCE_LOCATION")
        if (externalDataSource) {
            addToEnvironmentAsFirstSource(environment, externalDataSource)
        }
    }

    private static addToEnvironmentAsFirstSource(Environment environment, String groovyConfigFilePath) {
        def file = new File(groovyConfigFilePath)
        if (!file.exists()) {
            log.error("Configuration file ${groovyConfigFilePath} does not exist.")
            return
        }

        URL fileUrl = new URI("file:${groovyConfigFilePath}").toURL()
        ConfigObject config
        if (environment.activeProfiles) {
            if(environment.activeProfiles.length > 1) {
                log.warn("Expected to get single environment profile. But got: ${environment.activeProfiles} instead. " +
                        "Use ${environment.activeProfiles[0]} and ignore others")
            }
            config = new ConfigSlurper(environment.activeProfiles[0]).parse(fileUrl)
        } else {
            log.warn('No environment profile specified. All environment specific configurations will be ignored.')
            config = new ConfigSlurper().parse(fileUrl)
        }


        log.info "Including configuration file [${groovyConfigFilePath}] in configuration building."

        //navigable map represents the properties tree [a: [b: true] ] as usual property a.b=true
        def propertySource = new NavigableMap()
        propertySource.merge(config, false)
        //TODO Below is not proper way to add configurations. Find out better one.
        //http://grails.1312388.n4.nabble.com/Grails-3-External-config-td4658823.html
        environment.propertySources.addFirst(new NavigableMapPropertySource(groovyConfigFilePath, propertySource))
    }
}
