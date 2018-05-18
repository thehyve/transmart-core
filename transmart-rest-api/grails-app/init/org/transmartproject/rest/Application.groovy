package org.transmartproject.rest

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import grails.plugins.metadata.PluginSource
import grails.util.Environment
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.users.ProtectedOperation
import org.transmartproject.core.users.ProtectedResource
import org.transmartproject.core.users.User
import org.transmartproject.core.users.UsersResource
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

        @Autowired
        UsersResource usersResource

        @Delegate
        @Lazy
        User delegate = {
            usersResource.getUserFromUsername('user_-301')
        }()
    }
}

