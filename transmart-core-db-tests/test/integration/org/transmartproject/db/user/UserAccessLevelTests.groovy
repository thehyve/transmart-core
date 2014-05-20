package org.transmartproject.db.user

import org.gmock.WithGMock
import org.hibernate.SessionFactory
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.ontology.Study
import org.transmartproject.core.querytool.Item
import org.transmartproject.core.querytool.Panel
import org.transmartproject.core.querytool.QueryDefinition
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.users.ProtectedResource

import static groovy.util.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is
import static org.transmartproject.core.users.ProtectedOperation.WellKnownOperations.*
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

        // should fail even though the user is an admin and usually bypasses
        // all checks. The reason is we don't want to throw exceptions
        // only when a non-admin is used when by mistake where's checking
        // access for an unsupported ProtectedResource
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

    @Test
    void testQueryDefinitionUserHasAccessToOnePanelButNotAnother() {
        // it's enough to have access to one panel
        // fourth user has no access to study 2, but study 1 is public
        def fourthUser = accessLevelTestData.users[3]

        QueryDefinition definition = new QueryDefinition([
                new Panel(items: [new Item(
                        conceptKey: getStudy(STUDY2).ontologyTerm.key,
                )]),
                new Panel(items: [new Item(
                        conceptKey: getStudy(STUDY1).ontologyTerm.key,
                )]),
        ])

        assertThat fourthUser.canPerform(BUILD_COHORT, definition), is(true)
    }

    @Test
    void testQueryDefinitionUserHasNoAccessToAnyPanel() {
        // it's enough to have access to one panel
        // fourth user has no access to study 2, but study 1 is public
        def fourthUser = accessLevelTestData.users[3]

        QueryDefinition definition = new QueryDefinition([
                new Panel(items: [
                        new Item(
                                conceptKey: getStudy(STUDY2).ontologyTerm.key
                        )]),
                ])

        assertThat fourthUser.canPerform(BUILD_COHORT, definition), is(false)
    }

    @Test
    void testQueryDefinitionNonTopNode() {
        // test for bug where checking access only worked on the study top node
        // study 1 is public
        def thirdUser = accessLevelTestData.users[2]

        QueryDefinition definition = new QueryDefinition([
                new Panel(items: [
                        new Item(
                                conceptKey: '\\\\i2b2 main\\foo\\study1\\bar\\'
                        )]),
        ])

        assertThat thirdUser.canPerform(BUILD_COHORT, definition), is(true)
    }

    @Test
    void testDoNotAllowInvertedPanel() {
        def secondUser = accessLevelTestData.users[1]

        QueryDefinition definition = new QueryDefinition([
                new Panel(invert: true, items: [new Item(
                        conceptKey: getStudy(STUDY1).ontologyTerm.key,
                )]),
        ])

        assertThat secondUser.canPerform(BUILD_COHORT, definition), is(false)
    }

    @Test
    void testAllowInvertedPanelIfThereIsAnotherWithAccess() {
        def secondUser = accessLevelTestData.users[1]

        QueryDefinition definition = new QueryDefinition([
                new Panel(invert: true, items: [new Item(
                        conceptKey: getStudy(STUDY1).ontologyTerm.key,
                )]),
                new Panel(items: [new Item(
                        conceptKey: getStudy(STUDY2).ontologyTerm.key,
                )]),
        ])

        assertThat secondUser.canPerform(BUILD_COHORT, definition), is(true)
    }

    @Test
    void testQueryDefinitionAlwaysAllowAdministrator() {
        // first user is an admin
        def firstUser = accessLevelTestData.users[0]

        // normally would not be allowed because it's a single inverted panel
        QueryDefinition definition = new QueryDefinition([
                new Panel(invert: true, items: [new Item(
                        conceptKey: getStudy(STUDY1).ontologyTerm.key,
                )]),
        ])

        assertThat firstUser.canPerform(BUILD_COHORT, definition), is(true)
    }

    @Test
    void testQueryDefinitionNonStudyNodeIsDenied() {
        def secondUser = accessLevelTestData.users[1]

        /* non study nodes are typically parents to study nodes (e.g. 'Public
           Studies', so access to them should be denied, at least in the
           context of a query definition */
        QueryDefinition definition = new QueryDefinition([
                new Panel(items: [new Item(
                        conceptKey: '\\\\i2b2 main\\foo\\',
                )]),
        ])

        assertThat secondUser.canPerform(BUILD_COHORT, definition), is(false)
    }

    @Test
    void testQueryResultMismatch() {
        def secondUser = accessLevelTestData.users[1]
        def thirdUser = accessLevelTestData.users[2]

        QueryResult res = mock(QueryResult)
        res.getClass().returns QueryResult
        res.username.returns(secondUser.username).atLeastOnce()

        play {
            assertThat thirdUser.canPerform(READ, res), is(false)
        }
    }

    @Test
    void testQueryResultMatching() {
        def secondUser = accessLevelTestData.users[1]

        QueryResult res = mock(QueryResult)
        res.getClass().returns QueryResult
        res.username.returns secondUser.username

        play {
            assertThat secondUser.canPerform(READ, res), is(true)
        }
    }

    @Test
    void testQueryResultNonReadOperation() {
        def secondUser = accessLevelTestData.users[1]

        QueryResult res = mock(QueryResult)
        res.getClass().returns QueryResult

        play {
            shouldFail UnsupportedOperationException, {
                secondUser.canPerform(API_READ, res)
            }
        }
    }

    private Study getStudy(String name) {
        studiesResource.getStudyByName name
    }
}
