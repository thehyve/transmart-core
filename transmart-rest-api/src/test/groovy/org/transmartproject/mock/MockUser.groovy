package org.transmartproject.mock

import groovy.transform.CompileStatic
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.User

@CompileStatic
class MockUser implements User {

    protected boolean admin = false
    protected String username

    MockUser(String username) {
        this.username = username
    }

    @Override
    String getUsername() {
        return username
    }

    @Override
    String getRealName() {
        return null
    }

    @Override
    String getEmail() {
        return null
    }

    @Override
    boolean isAdmin() {
        return admin
    }

    @Override
    Map<String, PatientDataAccessLevel> getStudyToPatientDataAccessLevel() {
        return [:]
    }

}
