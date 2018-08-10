package org.transmartproject.db.clinical

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.PatientSetResource
import org.transmartproject.core.multidimquery.query.TrueConstraint
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.users.SimpleUser
import org.transmartproject.db.TestData
import spock.lang.Specification
import spock.lang.Unroll

import static org.transmartproject.core.users.PatientDataAccessLevel.MEASUREMENTS

@Integration
@Rollback
class PatientSetResourceSpec extends Specification {

    public static final String USERNAME = 'test-user'
    @Autowired
    PatientSetResource patientSetResource

    def setupData() {
        TestData.clearAllData()
        TestData.createHypercubeDefault().saveAll()
    }

    @Unroll
    def 'test user patient data access level change (admin: #beforHadAdminRole -> #hasAdminRole, studyToPatientDataAccessLevel: #beforeHadStudyPermissions -> #hasStudyPermissions) respected when reusing patient set'() {
        given: 'that we already build a patient set for such constraint for current user'
        setupData()
        def user = new SimpleUser(username: USERNAME, admin: beforHadAdminRole, studyToPatientDataAccessLevel: beforeHadStudyPermissions)
        def constraint = new TrueConstraint()
        QueryResult queryResultBefore = patientSetResource.createPatientSetQueryResult(
                'test user data access level',
                constraint,
                user,
                'v2',
                false)

        when: 'reuse flag is raised and user patient data access level has changed'
        user = new SimpleUser(username: USERNAME, admin: hasAdminRole, studyToPatientDataAccessLevel: hasStudyPermissions)
        QueryResult queryResultAfter = patientSetResource.createPatientSetQueryResult(
                'test user data access level',
                constraint,
                user,
                'v2',
                true)

        then: 'we build a new patient set regardless of the rised flag'
        queryResultAfter
        queryResultBefore != queryResultAfter
        queryResultBefore.setSize.compareTo(queryResultAfter.setSize) == patientSetSizeComparison

        where:
        beforHadAdminRole | beforeHadStudyPermissions          | hasAdminRole | hasStudyPermissions                | patientSetSizeComparison
        true              | [:]                                | false        | [:]                                | 1
        false             | ['EXP:sample study': MEASUREMENTS] | false        | [:]                                | 1
        false             | [:]                                | true         | [:]                                | -1
        false             | [:]                                | false        | ['EXP:sample study': MEASUREMENTS] | -1
    }
}
