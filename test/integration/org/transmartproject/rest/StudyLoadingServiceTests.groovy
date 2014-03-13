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

import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*

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
        accessLevelTestData = new AccessLevelTestData()
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
            assertThat testee.study, hasProperty('name',
                    equalTo(AccessLevelTestData.STUDY1))
        }
    }

    @Test
    void testGrantedAccessBecauseDirectlyGrantedAccess() {
        // third user has direct access to study 2
        user = accessLevelTestData.users[2]
        studyInRequest = AccessLevelTestData.STUDY2

        play {
            assertThat testee.study, hasProperty('name',
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

        assertThat testee.study, hasProperty('name',
                equalTo(AccessLevelTestData.STUDY2))
    }

    @Test
    void testGrantAccessWhenSpringSecurityInactive() {
        studyInRequest = AccessLevelTestData.STUDY2

        SpringSecurityUtils.securityConfig.active = false

        assertThat testee.study, hasProperty('name',
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
