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
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import grails.util.GrailsWebMockUtil
import grails.util.Holders
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.ApplicationContext
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.users.User
import org.transmartproject.db.user.AccessLevelTestData
import org.transmartproject.rest.misc.CurrentUser
import spock.lang.Ignore
import spock.lang.Specification

import static spock.util.matcher.HamcrestSupport.that
import static org.hamcrest.Matchers.*

@Ignore
@Integration
@Rollback
class StudyLoadingServiceSpec extends Specification {

    StudyLoadingService testee

    GrailsWebRequest grailsWebRequest

    AccessLevelTestData accessLevelTestData

    SpringSecurityService springSecurityServiceMock

    boolean originalSpringSecurityState

    //FIXME Setting bean properties with a map through data binding does not work here
    void setupData() {
        /* instantiate testee and do autowiring on it */
        testee = new StudyLoadingService()
        ApplicationContext appCtx = Holders.applicationContext

        AutowireCapableBeanFactory beanFactory =
                appCtx.autowireCapableBeanFactory

        // traditional autowiring
        beanFactory.autowireBeanProperties(testee,
                AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)
        // post-processor based autowiring
        beanFactory.autowireBean(testee)

        // the injected bean has scope request, which won't do
        testee.currentUser = new CurrentUser()
        beanFactory.autowireBean(testee.currentUser)

        /* mock web request */
        grailsWebRequest = GrailsWebMockUtil.bindMockWebRequest()

        /* insert test data */
        accessLevelTestData = AccessLevelTestData.createDefault()
        accessLevelTestData.saveAll()

        /* setup spring security service mock
         * maybe we could also use http://grails.org/plugin/spring-security-mock */
        springSecurityServiceMock = Mock(SpringSecurityService)
        testee.currentUser.springSecurityService = springSecurityServiceMock

        /* spring security is disabled in test env; activate it because the
         * testee skips the whole thing otherwise */
        originalSpringSecurityState = SpringSecurityUtils.securityConfig.active
        SpringSecurityUtils.securityConfig.active = true
    }

    void cleanup() {
        SpringSecurityUtils.securityConfig.active = originalSpringSecurityState
    }

    private void setUser(User user) {
        springSecurityServiceMock.isLoggedIn() >> true

        def principalMock = Mock(org.transmartproject.db.user.User)
        principalMock.username >> user.username

        springSecurityServiceMock.principal >> principalMock
    }

    private void setStudyInRequest(String studyName) {
        grailsWebRequest.params[StudyLoadingService.STUDY_ID_PARAM] = studyName
    }

    void testGrantedAccessBecauseAdmin() {
        setupData()
        //first user is an admin
        user = accessLevelTestData.users[0]
        studyInRequest = AccessLevelTestData.STUDY1

        expect:
        that testee.study, hasProperty('id',
                equalTo(AccessLevelTestData.STUDY1))
    }

    void testGrantedAccessBecauseDirectlyGrantedAccess() {
        setupData()
        // third user has direct access to study 2
        user = accessLevelTestData.users[2]
        studyInRequest = AccessLevelTestData.STUDY2

        expect:
        that testee.study, hasProperty('id',
                equalTo(AccessLevelTestData.STUDY2))
    }

    void testDeniedAccess() {
        setupData()
        // fourth user has no access to study 2
        user = accessLevelTestData.users[3]
        studyInRequest = AccessLevelTestData.STUDY2

        when:
        testee.study
        then:
        thrown(AccessDeniedException)
    }

    void testGrantAccessWhenNoSpringSecurityService() {
        setupData()
        studyInRequest = AccessLevelTestData.STUDY2

        testee.currentUser.springSecurityService = null

        expect:
        that testee.study, hasProperty('id',
                equalTo(AccessLevelTestData.STUDY2))
    }

    void testGrantAccessWhenSpringSecurityInactive() {
        setupData()
        studyInRequest = AccessLevelTestData.STUDY2

        SpringSecurityUtils.securityConfig.active = false

        expect:
        that testee.study, hasProperty('id',
                equalTo(AccessLevelTestData.STUDY2))
    }

    void testDeniedAccessWhenNotLoggedIn() {
        setupData()
        studyInRequest = AccessLevelTestData.STUDY1

        springSecurityServiceMock.isLoggedIn() >> false

        when:
        testee.study
        then:
        thrown(AccessDeniedException)
    }
}
