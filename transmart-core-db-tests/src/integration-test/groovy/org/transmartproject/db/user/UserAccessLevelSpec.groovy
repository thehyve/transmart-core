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
import org.transmartproject.core.ontology.MDStudiesResource
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.ontology.Study
import org.transmartproject.core.querytool.Item
import org.transmartproject.core.querytool.Panel
import org.transmartproject.core.querytool.QueryDefinition
import org.transmartproject.core.querytool.QueryResult
import spock.lang.Specification
import org.transmartproject.db.accesscontrol.AccessControlChecks
import org.transmartproject.db.ontology.I2b2Secure

import static org.hamcrest.Matchers.*
import static org.transmartproject.core.users.PatientDataAccessLevel.*
import static org.transmartproject.db.user.AccessLevelTestData.*

@Integration
@Rollback
class UserAccessLevelSpec extends Specification {

    @Autowired
    StudiesResource legacyStudiesResource

    @Autowired
    MDStudiesResource studiesResource

    @Autowired
    SessionFactory sessionFactory

    @Autowired
    AccessControlChecks accessControlChecks

    AccessLevelTestData accessLevelTestData = AccessLevelTestData.createDefault()

    void setupData() {
        accessLevelTestData.saveAll()
    }

    void testAdminAlwaysHasAccess() {
        setupData()
        User adminUser = accessLevelTestData.users[0]

        expect:
        accessControlChecks.canReadPatientData(adminUser, MEASUREMENTS, getLegacyStudy(STUDY1))
        accessControlChecks.canReadPatientData(adminUser, MEASUREMENTS, getLegacyStudy(STUDY2))
        accessControlChecks.canReadPatientData(adminUser, MEASUREMENTS, getLegacyStudy(STUDY3))
    }

    void testEveryoneHasAccessToPublicStudy() {
        setupData()
        // study1 is public
        expect:
        accessLevelTestData.users.every { accessControlChecks.canReadPatientData(it, MEASUREMENTS, getLegacyStudy(STUDY1)) }
    }

    void testPermissionViaGroup() {
        setupData()
        // second user is in group test_-201, which has access to study 2
        User secondUser = accessLevelTestData.users[1]
        def study2 = getLegacyStudy(STUDY2)

        expect:
        accessControlChecks.hasAccess(secondUser, study2)
        accessControlChecks.canReadPatientData(secondUser, MEASUREMENTS, study2)
    }

    void testDirectPermissionAssignment() {
        setupData()
        // third user has direct access to study 2
        User thirdUser = accessLevelTestData.users[2]

        expect:
        canRead == accessControlChecks.canReadPatientData(thirdUser, accLvl, getLegacyStudy(STUDY2))

        where:
        accLvl                | canRead
        MEASUREMENTS          | false
        COUNTS_WITH_THRESHOLD | true
    }

    void testAccessDeniedToUserWithoutPermission() {
        setupData()
        // fourth user has no access to study 2
        User fourthUser = accessLevelTestData.users[3]
        Study study2 = getLegacyStudy(STUDY2)

        expect:
        !accessControlChecks.hasAccess(fourthUser, study2)
        !accessControlChecks.canReadPatientData(fourthUser, MEASUREMENTS, study2)
    }

    void testAccessDeniedWhenOnlyViewPermission() {
        setupData()
        // fifth user has only SUMMARY permissions on study 2
        User fifthUser = accessLevelTestData.users[4]

        expect:
        !accessControlChecks.canReadPatientData(fifthUser, MEASUREMENTS, getLegacyStudy(STUDY2))
    }

    void testAccessGrantedWhenExportAndViewPermissionsExist() {
        setupData()
        // sixth user has both SUMMARY and MEASUREMENTS permissions on study2
        // the fact there's a SUMMARY permission shouldn't hide that
        // there is an MEASUREMENTS permission

        User sixthUser = accessLevelTestData.users[5]

        expect:
        accessControlChecks.canReadPatientData(sixthUser, MEASUREMENTS, getLegacyStudy(STUDY2))
    }

    void testEveryoneHasAccessViaEveryoneGroup() {
        setupData()

        expect:
        accessControlChecks.canReadPatientData(accessLevelTestData.users[userNumber], MEASUREMENTS, getLegacyStudy(study))

        where:
        userNumber | study
        0          | STUDY3
        1          | STUDY3
        2          | STUDY3
        3          | STUDY3
        4          | STUDY3
        5          | STUDY3
    }

    void testViewPermissionAndExportOperation() {
        setupData()
        // fifth user has only SUMMARY permissions on study 2
        User fifthUser = accessLevelTestData.users[4]

        expect:
        !accessControlChecks.canReadPatientData(fifthUser, MEASUREMENTS, getLegacyStudy(STUDY2))
    }

    void testViewPermissionAndShowInTableOperation() {
        setupData()
        // fifth user has only SUMMARY permissions on study 2
        User fifthUser = accessLevelTestData.users[4]

        expect:
        !accessControlChecks.canReadPatientData(fifthUser, MEASUREMENTS, getLegacyStudy(STUDY2))
    }

    void testViewPermissionAndShowInSummaryStatisticsOperation() {
        setupData()
        // fifth user has only SUMMARY permissions on study 2
        User fifthUser = accessLevelTestData.users[4]

        expect:
        accessControlChecks.canReadPatientData(fifthUser, SUMMARY, getLegacyStudy(STUDY2))
    }

    void testStudyWithoutI2b2Secure() {
        setupData()
        // such a study should be treated as public
        // fourth user has no access to study 2
        User fourthUser = accessLevelTestData.users[3]

        I2b2Secure.findByFullName(getLegacyStudy(STUDY2).ontologyTerm.fullName).
                delete(flush: true)

        expect:
        accessControlChecks.canReadPatientData(fourthUser, MEASUREMENTS, getLegacyStudy(STUDY2))
    }

    void testStudyWithEmptyToken() {
        setupData()
        // this should never happen. So we throw

        User fourthUser = accessLevelTestData.users[3]

        def i2b2Secure = I2b2Secure.findByFullName(getLegacyStudy(STUDY2).ontologyTerm.fullName)
        i2b2Secure.secureObjectToken = null

        when:
        accessControlChecks.canReadPatientData(fourthUser, MEASUREMENTS, getLegacyStudy(STUDY2))
        then:
        thrown(IllegalArgumentException)
    }

    void testGetAccessibleStudiesAdmin() {
        setupData()
        def adminUser = accessLevelTestData.users[0]

        def studies = legacyStudiesResource.getStudies(adminUser)
        expect:
        studies hasItems(
                hasProperty('id', equalTo(STUDY1)),
                hasProperty('id', equalTo(STUDY2)),
                hasProperty('id', equalTo(STUDY3)))
    }

    void testGetAccessibleStudiesPublicViaEveryoneGroup() {
        setupData()
        def fourthUser = accessLevelTestData.users[3]

        def studies = legacyStudiesResource.getStudies(fourthUser)
        expect:
        studies hasItem(hasProperty('id', equalTo(STUDY3)))
    }

    void testGetAccessibleStudiesPublicViaPublicSecureAccessToken() {
        setupData()
        def fourthUser = accessLevelTestData.users[3]

        def studies = legacyStudiesResource.getStudies(fourthUser)
        expect:
        studies hasItem(hasProperty('id', equalTo(STUDY1)))
    }

    void testGetAccessibleStudiesDeniedAccess() {
        setupData()
        // fourth user has no access to study 2
        def fourthUser = accessLevelTestData.users[3]

        def studies = legacyStudiesResource.getStudies(fourthUser)
        expect:
        studies not(hasItem(
                hasProperty('id', equalTo(STUDY2))))
    }

    void testGetAccessibleStudiesAccessViaNoI2b2Secure() {
        setupData()
        // fourth user has no access to study 2
        def fourthUser = accessLevelTestData.users[3]

        I2b2Secure.findByFullName(getLegacyStudy(STUDY2).ontologyTerm.fullName).
                delete(flush: true)

        def studies = legacyStudiesResource.getStudies(fourthUser)
        expect:
        studies hasItem(
                hasProperty('id', equalTo(STUDY2)))
    }

    void testGetAccessibleStudiesEmptySOTShouldThrow() {
        setupData()
        def fourthUser = accessLevelTestData.users[3]

        def i2b2Secure =
                I2b2Secure.findByFullName getLegacyStudy(STUDY2).ontologyTerm.fullName
        i2b2Secure.secureObjectToken = null

        when:
        legacyStudiesResource.getStudies(fourthUser)
        then:
        thrown(IllegalArgumentException)
    }

    void testQueryDefinitionUserHasAccessToOnePanelButNotAnother() {
        setupData()
        // it's enough to have access to one panel
        // fourth user has no access to study 2, but study 1 is public
        User fourthUser = accessLevelTestData.users[3]

        QueryDefinition definition = new QueryDefinition([
                new Panel(items: [new Item(
                        conceptKey: getLegacyStudy(STUDY2).ontologyTerm.key,
                )]),
                new Panel(items: [new Item(
                        conceptKey: getLegacyStudy(STUDY1).ontologyTerm.key,
                )]),
        ])

        expect:
        accessControlChecks.canRun(fourthUser, definition)
    }

    void testQueryDefinitionUserHasNoAccessToAnyPanel() {
        setupData()
        // it's enough to have access to one panel
        // fourth user has no access to study 2, but study 1 is public
        def fourthUser = accessLevelTestData.users[3]

        QueryDefinition definition = new QueryDefinition([
                new Panel(items: [
                        new Item(
                                conceptKey: getLegacyStudy(STUDY2).ontologyTerm.key
                        )]),
        ])

        expect:
        !accessControlChecks.canRun(fourthUser, definition)
    }

    void testQueryDefinitionNonTopNode() {
        setupData()
        // test for bug where checking access only worked on the study top node
        // study 1 is public
        User thirdUser = accessLevelTestData.users[2]

        QueryDefinition definition = new QueryDefinition([
                new Panel(items: [
                        new Item(
                                conceptKey: '\\\\i2b2 main\\foo\\study1\\bar\\'
                        )]),
        ])

        expect:
        accessControlChecks.canRun(thirdUser, definition)
    }

    void testDoNotAllowInvertedPanel() {
        setupData()

        User secondUser = accessLevelTestData.users[1]

        QueryDefinition definition = new QueryDefinition([
                new Panel(invert: true, items: [new Item(
                        conceptKey: getLegacyStudy(STUDY1).ontologyTerm.key,
                )]),
        ])

        expect:
        !accessControlChecks.canRun(secondUser, definition)
    }

    void testAllowInvertedPanelIfThereIsAnotherWithAccess() {
        setupData()
        User secondUser = accessLevelTestData.users[1]

        QueryDefinition definition = new QueryDefinition([
                new Panel(invert: true, items: [new Item(
                        conceptKey: getLegacyStudy(STUDY1).ontologyTerm.key,
                )]),
                new Panel(items: [new Item(
                        conceptKey: getLegacyStudy(STUDY2).ontologyTerm.key,
                )]),
        ])

        expect:
        accessControlChecks.canRun(secondUser, definition)
    }

    void testQueryDefinitionAlwaysAllowAdministrator() {
        setupData()
        // first user is an admin
        def firstUser = accessLevelTestData.users[0]

        // normally would not be allowed because it's a single inverted panel
        QueryDefinition definition = new QueryDefinition([
                new Panel(invert: true, items: [new Item(
                        conceptKey: getLegacyStudy(STUDY1).ontologyTerm.key,
                )]),
        ])

        expect:
        accessControlChecks.canRun(firstUser, definition)
    }

    void testQueryDefinitionNonStudyNodeIsDenied() {
        setupData()
        User secondUser = accessLevelTestData.users[1]

        /* non study nodes are typically parents to study nodes (e.g. 'Public
           Studies', so access to them should be denied, at least in the
           context of a query definition */
        QueryDefinition definition = new QueryDefinition([
                new Panel(items: [new Item(
                        conceptKey: '\\\\i2b2 main\\foo\\',
                )]),
        ])

        expect:
        !accessControlChecks.canRun(secondUser, definition)
    }

    void testQueryResultMismatch() {
        setupData()
        User secondUser = accessLevelTestData.users[1]
        User thirdUser = accessLevelTestData.users[2]

        QueryResult res = Mock(QueryResult)
        res.getClass() >> QueryResult
        res.username >> secondUser.username

        when:
        def can = accessControlChecks.hasAccess(thirdUser, res)

        then:
        (1.._) * res.username

        expect:
        !can
    }

    void testQueryResultMatching() {
        setupData()
        User secondUser = accessLevelTestData.users[1]

        QueryResult res = Mock(QueryResult)
        res.getClass() >> QueryResult
        res.username >> secondUser.username

        expect:
        accessControlChecks.hasAccess(secondUser, res)
    }

    void 'test get dimension studies for regular user'() {
        setupData()
        def thirdUser = accessLevelTestData.users[3]

        when:
        def studies = studiesResource.getStudies(thirdUser, minimalAccessLevel).toSorted { it.studyId }

        then:
        studies.size() == 2
        studies[0].studyId == 'study1'
        studies[0].secureObjectToken == org.transmartproject.db.i2b2data.Study.PUBLIC
        studies[1].studyId == 'study3'
    }

    void 'test get dimension studies for admin'() {
        setupData()
        def adminUser = accessLevelTestData.users[0]

        when:
        def studies = studiesResource.getStudies(adminUser, minimalAccessLevel)
        then:
        studies.size() == 3
    }

    private Study getLegacyStudy(String id) {
        legacyStudiesResource.getStudyById(id)
    }

}
