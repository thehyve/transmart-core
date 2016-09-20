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

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
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
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static org.transmartproject.core.users.ProtectedOperation.WellKnownOperations.*
import static org.transmartproject.db.user.AccessLevelTestData.*

@Integration
@Rollback
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
        adminUser.canPerform(API_READ, getStudy(STUDY1))
        adminUser.canPerform(API_READ, getStudy(STUDY2))
        adminUser.canPerform(API_READ, getStudy(STUDY3))
    }

    void testEveryoneHasAccessToPublicStudy() {
        setupData()
        // study1 is public
        expect:
        accessLevelTestData.users.every { it.canPerform(API_READ, getStudy(STUDY1)) }
    }

    void testPermissionViaGroup() {
        setupData()
        // second user is in group test_-201, which has access to study 2
        def secondUser = accessLevelTestData.users[1]

        expect:
        secondUser.canPerform(API_READ, getStudy(STUDY2))
    }

    void testDirectPermissionAssignment() {
        setupData()
        // third user has direct access to study 2
        def thirdUser = accessLevelTestData.users[2]

        expect:
        thirdUser.canPerform(API_READ, getStudy(STUDY2))
    }

    void testAccessDeniedToUserWithoutPermission() {
        setupData()
        // fourth user has no access to study 2
        def fourthUser = accessLevelTestData.users[3]

        expect:
        !fourthUser.canPerform(API_READ, getStudy(STUDY2))
    }

    void testAccessDeniedWhenOnlyViewPermission() {
        setupData()
        // fifth user has only VIEW permissions on study 2
        def fifthUser = accessLevelTestData.users[4]

        expect:
        !fifthUser.canPerform(API_READ, getStudy(STUDY2))
    }

    void testAccessGrantedWhenExportAndViewPermissionsExist() {
        setupData()
        // sixth user has both VIEW and EXPORT permissions on study2
        // the fact there's a VIEW permission shouldn't hide that
        // there is an EXPORT permission

        def sixthUser = accessLevelTestData.users[5]

        expect:
        sixthUser.canPerform(API_READ, getStudy(STUDY2))
    }

    void testEveryoneHasAccessViaEveryoneGroup() {
        setupData()
        // EVERYONE_GROUP has access to study 3

        expect:
        accessLevelTestData.users.every { it.canPerform(API_READ, getStudy(STUDY3)) }
    }

    void testWithUnsupportedProtectedResource() {
        setupData()
        def adminUser = accessLevelTestData.users[0]

        // should fail even though the user is an admin and usually bypasses
        // all checks. The reason is we don't want to throw exceptions
        // only when a non-admin is used when by mistake where's checking
        // access for an unsupported ProtectedResource
        when:
        adminUser.canPerform(API_READ, Mock(ProtectedResource))
        then:
        thrown(UnsupportedOperationException)
    }

    void testViewPermissionAndExportOperation() {
        setupData()
        // fifth user has only VIEW permissions on study 2
        def fifthUser = accessLevelTestData.users[4]

        expect:
        !fifthUser.canPerform(EXPORT, getStudy(STUDY2))
    }

    void testViewPermissionAndShowInTableOperation() {
        setupData()
        // fifth user has only VIEW permissions on study 2
        def fifthUser = accessLevelTestData.users[4]

        expect:
        fifthUser.canPerform(SHOW_IN_TABLE, getStudy(STUDY2))
        is(false)
    }

    void testViewPermissionAndShowInSummaryStatisticsOperation() {
        setupData()
        // fifth user has only VIEW permissions on study 2
        def fifthUser = accessLevelTestData.users[4]

        expect:
        fifthUser.canPerform(SHOW_SUMMARY_STATISTICS, getStudy(STUDY2))
    }

    void testStudyWithoutI2b2Secure() {
        setupData()
        // such a study should be treated as public
        // fourth user has no access to study 2
        def fourthUser = accessLevelTestData.users[3]

        I2b2Secure.findByFullName(getStudy(STUDY2).ontologyTerm.fullName).
                delete(flush: true)

        expect:
        fourthUser.canPerform(API_READ, getStudy(STUDY2))
    }

    void testStudyWithEmptyToken() {
        setupData()
        // this should never happen. So we throw

        def fourthUser = accessLevelTestData.users[3]

        def i2b2Secure = I2b2Secure.findByFullName(getStudy(STUDY2).ontologyTerm.fullName)
        i2b2Secure.secureObjectToken = null

        when:
        fourthUser.canPerform(API_READ, getStudy(STUDY2))
        then:
        thrown(UnexpectedResultException)
    }

    void testGetAccessibleStudiesAdmin() {
        setupData()
        def adminUser = accessLevelTestData.users[0]

        def studies = adminUser.accessibleStudies
        expect:
        studies hasItems(
                hasProperty('id', equalTo(STUDY1)),
                hasProperty('id', equalTo(STUDY2)),
                hasProperty('id', equalTo(STUDY3)))
    }

    void testGetAccessibleStudiesPublicViaEveryoneGroup() {
        setupData()
        def fourthUser = accessLevelTestData.users[3]

        def studies = fourthUser.accessibleStudies
        expect:
        studies hasItem(hasProperty('id', equalTo(STUDY3)))
    }

    void testGetAccessibleStudiesPublicViaPublicSecureAccessToken() {
        setupData()
        def fourthUser = accessLevelTestData.users[3]

        def studies = fourthUser.accessibleStudies
        expect:
        studies hasItem(hasProperty('id', equalTo(STUDY1)))
    }

    void testGetAccessibleStudiesDeniedAccess() {
        setupData()
        // fourth user has no access to study 2
        def fourthUser = accessLevelTestData.users[3]

        def studies = fourthUser.accessibleStudies
        expect:
        studies not(hasItem(
                hasProperty('id', equalTo(STUDY2))))
    }

    void testGetAccessibleStudiesAccessViaNoI2b2Secure() {
        setupData()
        // fourth user has no access to study 2
        def fourthUser = accessLevelTestData.users[3]

        I2b2Secure.findByFullName(getStudy(STUDY2).ontologyTerm.fullName).
                delete(flush: true)

        def studies = fourthUser.accessibleStudies
        expect:
        studies hasItem(
                hasProperty('id', equalTo(STUDY2)))
    }

    void testGetAccessibleStudiesEmptySOTShouldThrow() {
        setupData()
        def fourthUser = accessLevelTestData.users[3]

        def i2b2Secure =
                I2b2Secure.findByFullName getStudy(STUDY2).ontologyTerm.fullName
        i2b2Secure.secureObjectToken = null

        when:
        fourthUser.accessibleStudies
        then:
        thrown(UnexpectedResultException)
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

        expect:
        fourthUser.canPerform(BUILD_COHORT, definition)
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

        expect:
        !fourthUser.canPerform(BUILD_COHORT, definition)
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

        expect:
        thirdUser.canPerform(BUILD_COHORT, definition)
    }

    void testDoNotAllowInvertedPanel() {
        setupData()

        def secondUser = accessLevelTestData.users[1]

        QueryDefinition definition = new QueryDefinition([
                new Panel(invert: true, items: [new Item(
                        conceptKey: getStudy(STUDY1).ontologyTerm.key,
                )]),
        ])

        expect:
        secondUser.canPerform(BUILD_COHORT, definition)
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

        expect:
        secondUser.canPerform(BUILD_COHORT, definition)
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

        expect:
        firstUser.canPerform(BUILD_COHORT, definition)
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

        expect:
        !secondUser.canPerform(BUILD_COHORT, definition)
    }

    void testQueryResultMismatch() {
        setupData()
        def secondUser = accessLevelTestData.users[1]
        def thirdUser = accessLevelTestData.users[2]

        QueryResult res = Mock(QueryResult)
        res.getClass() >> QueryResult
        res.username >> secondUser.username.atLeastOnce()

        expect:
        !thirdUser.canPerform(READ, res)
    }

    void testQueryResultMatching() {
        setupData()
        def secondUser = accessLevelTestData.users[1]

        QueryResult res = Mock(QueryResult)
        res.getClass() >> QueryResult
        res.username >> secondUser.username

        expect:
        secondUser.canPerform(READ, res)
    }

    void testQueryResultNonReadOperation() {
        setupData()
        def secondUser = accessLevelTestData.users[1]

        QueryResult res = Mock(QueryResult)
        res.getClass() >> QueryResult

        when:
        secondUser.canPerform(API_READ, res)
        then:
        thrown(UnsupportedOperationException)
    }

    private Study getStudy(String id) {
        studiesResource.getStudyById id
    }
}
