package org.transmartproject.db.user

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification

import static org.transmartproject.core.users.PatientDataAccessLevel.MEASUREMENTS

@Rollback
@Integration
class UserSpec extends Specification {

    def 'test admin flag'() {
        expect:
        User.findByUsername(username).admin == hasAdminRights

        where:
        username             | hasAdminRights
        'admin'              | true
        'test-public-user-1' | false
    }

    def 'test study tokens and access levels'() {
        expect:
        User.findByUsername(username).studyToPatientDataAccessLevel == expectedAccessStudyTokenToAccLvl

        where:
        username             | expectedAccessStudyTokenToAccLvl
        'test-public-user-1' | [:]
        'test-public-user-2' | ['EXP:SHDCSCP': MEASUREMENTS, 'EXP:SCSCP': MEASUREMENTS]
    }
}
