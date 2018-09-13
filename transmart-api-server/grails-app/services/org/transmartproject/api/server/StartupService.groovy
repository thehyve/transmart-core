package org.transmartproject.api.server

import grails.util.Holders
import groovy.transform.CompileStatic
import org.springframework.boot.CommandLineRunner

@CompileStatic
class StartupService implements CommandLineRunner {

    @Override
    void run(String... args) throws Exception {
        ['keycloak.realm', 'keycloak.auth-server-url', 'keycloak.resource'].forEach({ String property ->
            def value = Holders.config.getProperty(property, String.class, '')
            if (!value) {
                throw new Exception("Property not configured correctly: ${property}. Specify a value in the external config file.")
            }
        })
        def disableTrustManager = Holders.config.getProperty('keycloak.disable-trust-manager', Boolean.class, false)
        if (disableTrustManager) {
            log.warn "SSL certificate checking for Keycloak is disabled! NEVER USE THIS IN PRODUCTION!"
        }
    }

}
