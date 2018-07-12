package org.transmartproject.mock

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.User

@CompileStatic
@EqualsAndHashCode(includes = ['username'])
@ToString(includeFields = true, includePackage = false)
class MockUser implements User {

    protected boolean admin = false
    protected String username
    Map<String, PatientDataAccessLevel> accessLevels = [:]

    MockUser(String username) {
        this.username = username
    }

    MockUser(String username, boolean admin) {
        this.username = username
        this.admin = admin
    }

    MockUser(String username, Map<String, PatientDataAccessLevel> accessLevels) {
        this.username = username
        this.accessLevels = accessLevels
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
        return this.accessLevels
    }

}
