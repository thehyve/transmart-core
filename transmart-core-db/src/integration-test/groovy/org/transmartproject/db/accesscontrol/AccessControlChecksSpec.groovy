package org.transmartproject.db.accesscontrol

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.users.LegacyAuthorisationChecks
import org.transmartproject.core.users.AuthorisationChecks
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.ontology.I2b2Secure
import org.transmartproject.db.user.User
import spock.lang.Specification

import static org.transmartproject.core.users.PatientDataAccessLevel.MEASUREMENTS
import static org.transmartproject.core.users.PatientDataAccessLevel.SUMMARY

@Rollback
@Integration
class AccessControlChecksSpec extends Specification {

    @Autowired
    AuthorisationChecks authorisationChecks

    def 'check access to a study'() {
        expect:
        canPerformReadOnTheStudy == authorisationChecks.hasAnyAccess(
                User.findByUsername(user),
                Study.findByStudyId(studyId)
        )

        where:
        user                 | studyId                        | canPerformReadOnTheStudy
        'test-public-user-1' | 'SHARED_CONCEPTS_STUDY_A'      | true
        'test-public-user-1' | 'SHARED_CONCEPTS_STUDY_C_PRIV' | false
        'test-public-user-2' | 'SHARED_CONCEPTS_STUDY_C_PRIV' | true
        'admin'              | 'SHARED_CONCEPTS_STUDY_C_PRIV' | true
        'test-public-user-1' | 'SHARED_CONCEPTS_STUDY_A'      | true
    }

    def 'check patient data access level'() {
        expect:
        canRead == authorisationChecks.canReadPatientData(
                User.findByUsername(user),
                dataType,
                Study.findByStudyId(studyId)
        )

        where:
        user                 | dataType     | studyId                        | canRead
        'test-public-user-1' | SUMMARY      | 'SHARED_CONCEPTS_STUDY_A'      | true
        'test-public-user-1' | MEASUREMENTS | 'SHARED_CONCEPTS_STUDY_A'      | true
        'test-public-user-1' | SUMMARY      | 'SHARED_CONCEPTS_STUDY_C_PRIV' | false
        'test-public-user-1' | MEASUREMENTS | 'SHARED_CONCEPTS_STUDY_C_PRIV' | false
        'test-public-user-2' | SUMMARY      | 'SHARED_CONCEPTS_STUDY_C_PRIV' | true
        'test-public-user-2' | MEASUREMENTS | 'SHARED_CONCEPTS_STUDY_C_PRIV' | true
        'admin'              | SUMMARY      | 'SHARED_CONCEPTS_STUDY_C_PRIV' | true
        'admin'              | MEASUREMENTS | 'SHARED_CONCEPTS_STUDY_C_PRIV' | true
    }

    @Autowired
    LegacyAuthorisationChecks legacyAuthorisationChecks

    def 'check access to a node'() {
        expect:
        canPerformReadOnTheNode == legacyAuthorisationChecks.hasAccess(
                User.findByUsername(user),
                I2b2Secure.findByFullName(node)
        )

        where:
        user                 | node                                                                 | canPerformReadOnTheNode
        'test-public-user-1' | '\\Public Studies\\SHARED_CONCEPTS_STUDY_A\\Demography\\Age\\'       | true
        'test-public-user-1' | '\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\Demography\\Age\\' | false
        'test-public-user-2' | '\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\Demography\\Age\\' | true
        'admin'              | '\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\Demography\\Age\\' | true
    }
}
