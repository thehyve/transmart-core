/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.user

import grails.test.mixin.TestMixin
import grails.test.mixin.integration.Integration
import grails.test.mixin.web.ControllerUnitTestMixin
import grails.transaction.Rollback
import groovy.util.logging.Slf4j
import spock.lang.Specification

import org.gmock.WithGMock
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.ontology.Study
import org.transmartproject.core.querytool.Item
import org.transmartproject.core.querytool.Panel
import org.transmartproject.core.querytool.QueryDefinition
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.users.ProtectedResource
import org.transmartproject.db.ontology.I2b2Secure

import static groovy.util.GroovyAssert.shouldFail
import static org.hamcrest.Matchers.*
import static org.transmartproject.core.users.ProtectedOperation.WellKnownOperations.*
import static org.transmartproject.db.user.AccessLevelTestData.*

@TestMixin(ControllerUnitTestMixin)
@WithGMock
@Integration
@Rollback
@Slf4j
class UserAccessLevelSpec extends Specification {

    @Autowired
    StudiesResource studiesResource

    @Autowired
    SessionFactory sessionFactory

    AccessLevelTestData accessLevelTestData = AccessLevelTestData.createDefault()

    void setupData() {
        accessLevelTestData.saveAll()
        sessionFactory.currentSession.flush()
    }

    void testAdminAlwaysHasAccess() {
        setupData()
        def adminUser = accessLevelTestData.users[0]

        expect:
            adminUser.canPerform(API_READ, getStudy(STUDY1)) is(true)
            adminUser.canPerform(API_READ, getStudy(STUDY2)) is(true)
            adminUser.canPerform(API_READ, getStudy(STUDY3)) is(true)
    }

    void testEveryoneHasAccessToPublicStudy() {
        setupData()
        // study1 is public
        accessLevelTestData.users.each { u ->
            println "Testing for user $u"
            expect: u.canPerform(API_READ, getStudy(STUDY1)) is(true)
            println "Passed"
        }
    }

    void testPermissionViaGroup() {
        setupData()
        // second user is in group test_-201, which has access to study 2
        def secondUser = accessLevelTestData.users[1]

        expect: secondUser.canPerform(API_READ, getStudy(STUDY2)) is(true)
    }

    void testDirectPermissionAssignment() {
        setupData()
        // third user has direct access to study 2
        def thirdUser = accessLevelTestData.users[2]

        expect: thirdUser.canPerform(API_READ, getStudy(STUDY2)) is(true)
    }

    void testAccessDeniedToUserWithoutPermission() {
        setupData()
        // fourth user has no access to study 2
        def fourthUser = accessLevelTestData.users[3]

        expect: fourthUser.canPerform(API_READ, getStudy(STUDY2)) is(false)
    }

    void testAccessDeniedWhenOnlyViewPermission() {
        setupData()
        // fifth user has only VIEW permissions on study 2
        def fifthUser = accessLevelTestData.users[4]

        expect: fifthUser.canPerform(API_READ, getStudy(STUDY2)) is(false)
    }

    void testAccessGrantedWhenExportAndViewPermissionsExist() {
        setupData()
        // sixth user has both VIEW and EXPORT permissions on study2
        // the fact there's a VIEW permission shouldn't hide that
        // there is an EXPORT permission

        def sixthUser = accessLevelTestData.users[5]

        expect: sixthUser.canPerform(API_READ, getStudy(STUDY2)) is(true)
    }

    void testEveryoneHasAccessViaEveryoneGroup() {
        setupData()
        // EVERYONE_GROUP has access to study 3

        accessLevelTestData.users.each { u ->
            println "Testing for user $u"
            expect: u.canPerform(API_READ, getStudy(STUDY3)) is(true)
            println "Passed"
        }
    }

    void testWithUnsupportedProtectedResource() {
        setupData()
        def adminUser = accessLevelTestData.users[0]

        // should fail even though the user is an admin and usually bypasses
        // all checks. The reason is we don't want to throw exceptions
        // only when a non-admin is used when by mistake where's checking
        // access for an unsupported ProtectedResource
        shouldFail UnsupportedOperationException, {
            adminUser.canPerform(API_READ, mock(ProtectedResource))
        }
    }

    void testViewPermissionAndExportOperation() {
        setupData()
        // fifth user has only VIEW permissions on study 2
        def fifthUser = accessLevelTestData.users[4]

        expect: fifthUser.canPerform(EXPORT, getStudy(STUDY2)) is(false)
    }

    void testViewPermissionAndShowInTableOperation() {
        setupData()
        // fifth user has only VIEW permissions on study 2
        def fifthUser = accessLevelTestData.users[4]

        expect: fifthUser.canPerform(SHOW_IN_TABLE, getStudy(STUDY2))
                   is(false)
    }

    void testViewPermissionAndShowInSummaryStatisticsOperation() {
        setupData()
        // fifth user has only VIEW permissions on study 2
        def fifthUser = accessLevelTestData.users[4]

        expect: fifthUser.canPerform(SHOW_SUMMARY_STATISTICS, getStudy(STUDY2)) is(true)
    }

    void testStudyWithoutI2b2Secure() {
        setupData()
        // such a study should be treated as public
        // fourth user has no access to study 2
        def fourthUser = accessLevelTestData.users[3]

        I2b2Secure.findByFullName(getStudy(STUDY2).ontologyTerm.fullName).
                delete(flush: true)

        expect: fourthUser.canPerform(API_READ, getStudy(STUDY2)) is(true)
    }

    void testStudyWithEmptyToken() {
        setupData()
        // this should never happen. So we throw

        def fourthUser = accessLevelTestData.users[3]

        def i2b2Secure = I2b2Secure.findByFullName(getStudy(STUDY2).ontologyTerm.fullName)
        i2b2Secure.secureObjectToken = null

        shouldFail UnexpectedResultException, {
            fourthUser.canPerform(API_READ, getStudy(STUDY2))
        }
    }

    void testGetAccessibleStudiesAdmin() {
        setupData()
        def adminUser = accessLevelTestData.users[0]

        def studies = adminUser.accessibleStudies
        expect: studies hasItems(
                hasProperty('id', equalTo(STUDY1)),
                hasProperty('id', equalTo(STUDY2)),
                hasProperty('id', equalTo(STUDY3)))
    }

    void testGetAccessibleStudiesPublicViaEveryoneGroup() {
        setupData()
        def fourthUser = accessLevelTestData.users[3]

        def studies = fourthUser.accessibleStudies
        expect: studies hasItem(hasProperty('id', equalTo(STUDY3)))
    }

    void testGetAccessibleStudiesPublicViaPublicSecureAccessToken() {
        setupData()
        def fourthUser = accessLevelTestData.users[3]

        def studies = fourthUser.accessibleStudies
        expect: studies hasItem(hasProperty('id', equalTo(STUDY1)))
    }

    void testGetAccessibleStudiesDeniedAccess() {
        setupData()
        // fourth user has no access to study 2
        def fourthUser = accessLevelTestData.users[3]

        def studies = fourthUser.accessibleStudies
        expect: studies not(hasItem(
                hasProperty('id', equalTo(STUDY2))))
    }

    void testGetAccessibleStudiesAccessViaNoI2b2Secure() {
        setupData()
        // fourth user has no access to study 2
        def fourthUser = accessLevelTestData.users[3]

        I2b2Secure.findByFullName(getStudy(STUDY2).ontologyTerm.fullName).
                delete(flush: true)

        def studies = fourthUser.accessibleStudies
        expect: studies hasItem(
                hasProperty('id', equalTo(STUDY2)))
    }

    void testGetAccessibleStudiesEmptySOTShouldThrow() {
        setupData()
        def fourthUser = accessLevelTestData.users[3]

        def i2b2Secure =
                I2b2Secure.findByFullName getStudy(STUDY2).ontologyTerm.fullName
        i2b2Secure.secureObjectToken = null

        shouldFail UnexpectedResultException, {
            fourthUser.accessibleStudies
        }
    }

    void testQueryDefinitionUserHasAccessToOnePanelButNotAnother() {
        setupData()
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

        expect: fourthUser.canPerform(BUILD_COHORT, definition) is(true)
    }

    void testQueryDefinitionUserHasNoAccessToAnyPanel() {
        setupData()
        // it's enough to have access to one panel
        // fourth user has no access to study 2, but study 1 is public
        def fourthUser = accessLevelTestData.users[3]

        QueryDefinition definition = new QueryDefinition([
                new Panel(items: [
                        new Item(
                                conceptKey: getStudy(STUDY2).ontologyTerm.key
                        )]),
                ])

        expect: fourthUser.canPerform(BUILD_COHORT, definition) is(false)
    }

    void testQueryDefinitionNonTopNode() {
        setupData()
        // test for bug where checking access only worked on the study top node
        // study 1 is public
        def thirdUser = accessLevelTestData.users[2]

        QueryDefinition definition = new QueryDefinition([
                new Panel(items: [
                        new Item(
                                conceptKey: '\\\\i2b2 main\\foo\\study1\\bar\\'
                        )]),
        ])

        expect: thirdUser.canPerform(BUILD_COHORT, definition) is(true)
    }

    void testDoNotAllowInvertedPanel() {
        setupData()

        def secondUser = accessLevelTestData.users[1]

        QueryDefinition definition = new QueryDefinition([
                new Panel(invert: true, items: [new Item(
                        conceptKey: getStudy(STUDY1).ontologyTerm.key,
                )]),
        ])

        expect: secondUser.canPerform(BUILD_COHORT, definition) is(false)
    }

    void testAllowInvertedPanelIfThereIsAnotherWithAccess() {
        setupData()
        def secondUser = accessLevelTestData.users[1]

        QueryDefinition definition = new QueryDefinition([
                new Panel(invert: true, items: [new Item(
                        conceptKey: getStudy(STUDY1).ontologyTerm.key,
                )]),
                new Panel(items: [new Item(
                        conceptKey: getStudy(STUDY2).ontologyTerm.key,
                )]),
        ])

        expect: secondUser.canPerform(BUILD_COHORT, definition) is(true)
    }

    void testQueryDefinitionAlwaysAllowAdministrator() {
        setupData()
        // first user is an admin
        def firstUser = accessLevelTestData.users[0]

        // normally would not be allowed because it's a single inverted panel
        QueryDefinition definition = new QueryDefinition([
                new Panel(invert: true, items: [new Item(
                        conceptKey: getStudy(STUDY1).ontologyTerm.key,
                )]),
        ])

        expect: firstUser.canPerform(BUILD_COHORT, definition) is(true)
    }

    void testQueryDefinitionNonStudyNodeIsDenied() {
        setupData()
        def secondUser = accessLevelTestData.users[1]

        /* non study nodes are typically parents to study nodes (e.g. 'Public
           Studies', so access to them should be denied, at least in the
           context of a query definition */
        QueryDefinition definition = new QueryDefinition([
                new Panel(items: [new Item(
                        conceptKey: '\\\\i2b2 main\\foo\\',
                )]),
        ])

        expect: secondUser.canPerform(BUILD_COHORT, definition) is(false)
    }

    void testQueryResultMismatch() {
        setupData()
        def secondUser = accessLevelTestData.users[1]
        def thirdUser = accessLevelTestData.users[2]

        QueryResult res = mock(QueryResult)
        res.getClass().returns QueryResult
        res.username.returns(secondUser.username).atLeastOnce()

        play {
            expect: thirdUser.canPerform(READ, res) is(false)
        }
    }

    void testQueryResultMatching() {
        setupData()
        def secondUser = accessLevelTestData.users[1]

        QueryResult res = mock(QueryResult)
        res.getClass().returns QueryResult
        res.username.returns secondUser.username

        play {
            expect: secondUser.canPerform(READ, res) is(true)
        }
    }

    void testQueryResultNonReadOperation() {
        setupData()
        def secondUser = accessLevelTestData.users[1]

        QueryResult res = mock(QueryResult)
        res.getClass().returns QueryResult

        play {
            shouldFail UnsupportedOperationException, {
                secondUser.canPerform(API_READ, res)
            }
        }
    }

    private Study getStudy(String id) {
        studiesResource.getStudyById id
    }
}
