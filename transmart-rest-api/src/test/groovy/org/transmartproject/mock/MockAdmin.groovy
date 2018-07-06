package org.transmartproject.mock

import groovy.transform.CompileStatic

@CompileStatic
class MockAdmin extends MockUser {

    MockAdmin(String username) {
        super(username)
        admin = true
    }

}
