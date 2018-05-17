package org.transmartproject.rest

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import grails.plugins.metadata.PluginSource
import grails.util.Environment
import org.transmartproject.core.users.ProtectedOperation
import org.transmartproject.core.users.ProtectedResource
import org.transmartproject.core.users.User
import org.transmartproject.rest.misc.CurrentUser

@PluginSource
class Application extends GrailsAutoConfiguration {

    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }

    @Override
    Closure doWithSpring() {
        if (Environment.current == Environment.TEST) {
            return { ->
                //Override the current user with test one
                currentUser(DummyTestAdministrator)
            }
        }
    }

    static class DummyTestAdministrator extends CurrentUser {

        /* These correspond to the properties of the default transmart
         * administrator user */
        final Long id = 1
        final String username = 'user_-301'
        final String realName = 'Sys Admin'
        final String email = ''

        @Override
        boolean canPerform(ProtectedOperation operation, ProtectedResource protectedResource) {
            true
        }
    }
}

