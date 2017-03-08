package org.transmart.oauth

import grails.boot.*
import grails.boot.config.GrailsAutoConfiguration
import grails.plugins.metadata.*
import groovy.util.logging.Slf4j
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.security.SecurityFilterAutoConfiguration

@Slf4j
@EnableAutoConfiguration(exclude = [SecurityFilterAutoConfiguration])
class Application extends GrailsAutoConfiguration {
    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }
}