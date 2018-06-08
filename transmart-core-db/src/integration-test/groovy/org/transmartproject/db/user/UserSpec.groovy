package org.transmartproject.db.user

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification

import static org.transmartproject.core.users.ProtectedOperation.WellKnownOperations.*

@Rollback
@Integration
class UserSpec extends Specification {

    static viewOperations = [BUILD_COHORT, SHOW_SUMMARY_STATISTICS, RUN_ANALYSIS]

    def 'test admin flag'() {
        expect:
        User.findByUsername(username).admin == hasAdminRights

        where:
        username             | hasAdminRights
        'admin'              | true
        'test-public-user-1' | false
    }

    def 'test study tokens and operations'() {
        when:
        def accessStudyTokenToOperations = User.findByUsername(username).accessStudyTokenToOperations
        then:
        accessStudyTokenToOperations.asMap() == expectedAccessStudyTokenToOperations

        where:
        username             | expectedAccessStudyTokenToOperations
        'test-public-user-1' | [:]
        'test-public-user-2' | ['EXP:SHDCSCP': viewOperations, 'EXP:SCSCP': viewOperations]
    }
}
