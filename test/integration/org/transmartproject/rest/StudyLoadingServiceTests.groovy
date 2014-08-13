/*
 * Copyright 2014 Janssen Research & Development, LLC.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version, along with the following terms:
 *
 *   1. You may convey a work based on this program in accordance with
 *      section 5, provided that you retain the above notices.
 *   2. You may convey verbatim copies of this program code as you receive
 *      it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.rest

import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import grails.util.GrailsWebUtil
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext
import org.codehaus.groovy.grails.test.runner.phase.IntegrationTestPhaseConfigurer
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.gmock.WithGMock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.users.User
import org.transmartproject.db.user.AccessLevelTestData

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasProperty

@TestMixin(IntegrationTestMixin)
@WithGMock
class StudyLoadingServiceTests {

    StudyLoadingService testee

    GrailsWebRequest grailsWebRequest

    AccessLevelTestData accessLevelTestData

    SpringSecurityService springSecurityServiceMock

    boolean originalSpringSecurityState

    @Before
    void setUp() {
        /* instantiate testee and do autowiring on it */
        testee = new StudyLoadingService()
        GrailsWebApplicationContext appCtx =
                IntegrationTestPhaseConfigurer.currentApplicationContext

        ConfigurableListableBeanFactory beanFactory =
                appCtx.beanFactory

        // traditional autowiring
        beanFactory.autowireBeanProperties(testee,
                AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)
        // post-processor based autowiring
        beanFactory.autowireBean(testee)

        /* mock web request */
        grailsWebRequest = GrailsWebUtil.bindMockWebRequest()

        /* insert test data */
        accessLevelTestData = AccessLevelTestData.createDefault()
        accessLevelTestData.saveAll()

        /* setup spring security service mock
         * maybe we could also use http://grails.org/plugin/spring-security-mock */
        springSecurityServiceMock = mock(SpringSecurityService)
        testee.springSecurityService = springSecurityServiceMock

        /* spring security is disabled in test env; activate it because the
         * testee skips the whole thing otherwise */
        originalSpringSecurityState = SpringSecurityUtils.securityConfig.active
        SpringSecurityUtils.securityConfig.active = true
     }

    @After
    void tearDown() {
        SpringSecurityUtils.securityConfig.active = originalSpringSecurityState
    }

    private void setUser(User user) {
        springSecurityServiceMock.isLoggedIn().returns true

        def principalMock = mock()
        principalMock.username.returns user.username

        springSecurityServiceMock.principal.returns principalMock
    }

    private void setStudyInRequest(String studyName) {
        grailsWebRequest.params[StudyLoadingService.STUDY_ID_PARAM] = studyName
    }

    @Test
    void testGrantedAccessBecauseAdmin() {
        //first user is an admin
        user = accessLevelTestData.users[0]
        studyInRequest = AccessLevelTestData.STUDY1

        play {
            assertThat testee.study, hasProperty('id',
                    equalTo(AccessLevelTestData.STUDY1))
        }
    }

    @Test
    void testGrantedAccessBecauseDirectlyGrantedAccess() {
        // third user has direct access to study 2
        user = accessLevelTestData.users[2]
        studyInRequest = AccessLevelTestData.STUDY2

        play {
            assertThat testee.study, hasProperty('id',
                    equalTo(AccessLevelTestData.STUDY2))
        }
    }

    @Test
    void testDeniedAccess() {
        // fourth user has no access to study 2
        user = accessLevelTestData.users[3]
        studyInRequest = AccessLevelTestData.STUDY2

        play {
            shouldFail AccessDeniedException, {
                testee.study
            }
        }
    }

    @Test
    void testGrantAccessWhenNoSpringSecurityService() {
        studyInRequest = AccessLevelTestData.STUDY2

        testee.springSecurityService = null

        assertThat testee.study, hasProperty('id',
                equalTo(AccessLevelTestData.STUDY2))
    }

    @Test
    void testGrantAccessWhenSpringSecurityInactive() {
        studyInRequest = AccessLevelTestData.STUDY2

        SpringSecurityUtils.securityConfig.active = false

        assertThat testee.study, hasProperty('id',
                equalTo(AccessLevelTestData.STUDY2))
    }

    @Test
    void testDeniedAccessWhenNotLoggedIn() {
        studyInRequest = AccessLevelTestData.STUDY1

        springSecurityServiceMock.isLoggedIn().returns false

        play {
            shouldFail AccessDeniedException, {
                testee.study
            }
        }
    }
}
