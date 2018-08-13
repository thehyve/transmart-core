package org.transmartproject.db.clinical

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.multidimquery.PatientSetResource
import org.transmartproject.core.multidimquery.query.PatientSetConstraint
import org.transmartproject.core.multidimquery.query.StudyNameConstraint
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

    public static final String USERNAME1 = 'test-user1'
    public static final String USERNAME2 = 'test-user2'

    @Autowired
    PatientSetResource patientSetResource

    @Autowired
    MultiDimensionalDataResource multiDimService

    def setupData() {
        TestData.clearAllData()
        TestData.createHypercubeDefault().saveAll()
    }

    @Unroll
    def 'test user patient data access level change (admin: #beforHadAdminRole -> #hasAdminRole, studyToPatientDataAccessLevel: #beforeHadStudyPermissions -> #hasStudyPermissions) respected when reusing patient set'() {
        given: 'that we already build a patient set for such constraint for current user'
        setupData()
        def user = new SimpleUser(username: USERNAME1, admin: beforHadAdminRole, studyToPatientDataAccessLevel: beforeHadStudyPermissions)
        def constraint = new TrueConstraint()
        QueryResult queryResultBefore = patientSetResource.createPatientSetQueryResult(
                'test user data access level',
                constraint,
                user,
                'v2',
                false)

        when: 'reuse flag is raised and user patient data access level has changed'
        user = new SimpleUser(username: USERNAME1, admin: hasAdminRole, studyToPatientDataAccessLevel: hasStudyPermissions)
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

    @Unroll
    def 'test patient sets between users with identical permissions are the same when reusePatientSet=#reusePatientSet'() {
        given:
        setupData()
        def user1 = new SimpleUser(username: USERNAME1, admin: false, studyToPatientDataAccessLevel: ['EXP:sample study': MEASUREMENTS])
        def user2 = new SimpleUser(username: USERNAME2, admin: false, studyToPatientDataAccessLevel: ['EXP:sample study': MEASUREMENTS])
        def constraint = new StudyNameConstraint(studyId: 'sample study')
        def patientDim = multiDimService.getDimension('patient')
        QueryResult queryResultForUser1 = patientSetResource.createPatientSetQueryResult(
                'test user1 data access level',
                constraint,
                user1,
                'v2',
                false)
        Set<Patient> patientsForUser1 = multiDimService.getDimensionElements(patientDim,
                new PatientSetConstraint(patientSetId: queryResultForUser1.id), user1).toList() as Set<Patient>

        when:
        QueryResult queryResultForUser2 = patientSetResource.createPatientSetQueryResult(
                'test user1 data access level',
                constraint,
                user2,
                'v2',
                reusePatientSet)
        Set<Patient> patientsForUser2 = multiDimService.getDimensionElements(patientDim,
                new PatientSetConstraint(patientSetId: queryResultForUser2.id), user2).toList() as Set<Patient>


        then:
        patientsForUser1 == patientsForUser2

        where:
        reusePatientSet << [true, false]
    }
}
