package org.transmartproject.db.user

import org.gmock.WithGMock
import org.hibernate.SessionFactory
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.ontology.Study
import org.transmartproject.core.users.ProtectedResource

import static groovy.util.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is
import static org.transmartproject.core.users.ProtectedOperation.WellKnownOperations.API_READ
import static org.transmartproject.core.users.ProtectedOperation.WellKnownOperations.EXPORT
import static org.transmartproject.core.users.ProtectedOperation.WellKnownOperations.SHOW_IN_TABLE
import static org.transmartproject.core.users.ProtectedOperation.WellKnownOperations.SHOW_SUMMARY_STATISTICS
import static org.transmartproject.db.user.AccessLevelTestData.*

@WithGMock
class UserAccessLevelTests {

    @Autowired
    StudiesResource studiesResource

    @Autowired
    SessionFactory sessionFactory

    AccessLevelTestData accessLevelTestData = new AccessLevelTestData()

    @Before
    void setUp() {
        accessLevelTestData.saveAll()
        sessionFactory.currentSession.flush()
    }

    @Test
    void testAdminAlwaysHasAccess() {
        def adminUser = accessLevelTestData.users[0]

        assertThat adminUser.canPerform(API_READ, getStudy(STUDY1)), is(true)
        assertThat adminUser.canPerform(API_READ, getStudy(STUDY2)), is(true)
        assertThat adminUser.canPerform(API_READ, getStudy(STUDY3)), is(true)
    }

    @Test
    void testEveryoneHasAccessToPublicStudy() {
        // study1 is public
        accessLevelTestData.users.each { u ->
            println "Testing for user $u"
            assertThat u.canPerform(API_READ, getStudy(STUDY1)), is(true)
            println "Passed"
        }
    }

    @Test
    void testPermissionViaGroup() {
        // second user is in group test_-201, which has access to study 2
        def secondUser = accessLevelTestData.users[1]

        assertThat secondUser.canPerform(API_READ, getStudy(STUDY2)), is(true)
    }

    @Test
    void testDirectPermissionAssignment() {
        // third user has direct access to study 2
        def thirdUser = accessLevelTestData.users[2]

        assertThat thirdUser.canPerform(API_READ, getStudy(STUDY2)), is(true)
    }

    @Test
    void testAccessDeniedToUserWithoutPermission() {
        // fourth user has no access to study 2
        def fourthUser = accessLevelTestData.users[3]

        assertThat fourthUser.canPerform(API_READ, getStudy(STUDY2)), is(false)
    }

    @Test
    void testAccessDeniedWhenOnlyViewPermission() {
        // fifth user has only VIEW permissions on study 2
        def fifthUser = accessLevelTestData.users[4]

        assertThat fifthUser.canPerform(API_READ, getStudy(STUDY2)), is(false)
    }

    @Test
    void testAccessGrantedWhenExportAndViewPermissionsExist() {
        // sixth user has both VIEW and EXPORT permissions on study2
        // the fact there's a VIEW permission shouldn't hide that
        // there is an EXPORT permission

        def sixthUser = accessLevelTestData.users[5]

        assertThat sixthUser.canPerform(API_READ, getStudy(STUDY2)), is(true)
    }

    @Test
    void testEveryoneHasAccessViaEveryoneGroup() {
        // EVERYONE_GROUP has access to study 3

        accessLevelTestData.users.each { u ->
            println "Testing for user $u"
            assertThat u.canPerform(API_READ, getStudy(STUDY3)), is(true)
            println "Passed"
        }
    }

    @Test
    void testWithUnsupportedProtectedResource() {
        def adminUser = accessLevelTestData.users[0]

        shouldFail UnsupportedOperationException, {
            adminUser.canPerform(API_READ, mock(ProtectedResource))
        }
    }

    @Test
    void testViewPermissionAndExportOperation() {
        // fifth user has only VIEW permissions on study 2
        def fifthUser = accessLevelTestData.users[4]

        assertThat fifthUser.canPerform(EXPORT, getStudy(STUDY2)), is(false)
    }

    @Test
    void testViewPermissionAndShowInTableOperation() {
        // fifth user has only VIEW permissions on study 2
        def fifthUser = accessLevelTestData.users[4]

        assertThat fifthUser.canPerform(SHOW_IN_TABLE, getStudy(STUDY2)),
                   is(false)
    }

    @Test
    void testViewPermissionAndShowInSummaryStatisticsOperation() {
        // fifth user has only VIEW permissions on study 2
        def fifthUser = accessLevelTestData.users[4]

        assertThat fifthUser.canPerform(SHOW_SUMMARY_STATISTICS, getStudy(STUDY2)), is(true)
    }

    private Study getStudy(String name) {
        studiesResource.getStudyByName name
    }
}
