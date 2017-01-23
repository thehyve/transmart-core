package com.recomdata.transmart.rmodules

import com.recomdata.transmart.data.association.RModulesOutputRenderService
import grails.test.mixin.TestFor
import org.gmock.WithGMock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.exceptions.InvalidRequestException

import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@TestFor(AnalysisFilesController)
@WithGMock
class AnalysisFilesControllerTests {

    private static final String USER_NAME = 'user'
    private static final String OTHER_USER_NAME = 'other_user'
    private static final String ADMIN_NAME = 'admin'
    private static final String EXISTING_FILE_NAME = 'file_that_exists'
    private static final String FILE_CONTENTS = 'file contents\n'
    private static final String ANALYSIS_NAME = "$USER_NAME-Analysis-100"

    File temporaryDirectory
    File analysisDirectory
    File targetFile
    def sendFileServiceMock
    def mockGrailsUser

    @Before
    void before() {
        temporaryDirectory = File.createTempDir('analysis_file_test', '')
        analysisDirectory = new File(temporaryDirectory, ANALYSIS_NAME)
        analysisDirectory.mkdir()

        controller.RModulesOutputRenderService = mock RModulesOutputRenderService
        controller.RModulesOutputRenderService.tempFolderDirectory.
                returns(temporaryDirectory.absolutePath).stub()

        sendFileServiceMock = mock()
        controller.sendFileService = sendFileServiceMock

        mockGrailsUser = mock()
        controller.springSecurityService = mock()
        controller.springSecurityService.principal.
                returns(mockGrailsUser).stub()

        params.analysisName = ANALYSIS_NAME
    }

    void setTestUsername(String username) {
        mockGrailsUser.username.returns username
    }

    void setAdmin(boolean value) {
        if (value) {
            def adminAuthority = mock()
            adminAuthority.authority.returns AnalysisFilesController.ROLE_ADMIN
            mockGrailsUser.authorities.returns([adminAuthority])
        } else {
            mockGrailsUser.authorities.returns([])
        }
    }

    void setFile(String filename) {
        targetFile = new File(analysisDirectory, filename)
        targetFile << FILE_CONTENTS

        params.path = filename
    }

    @After
    void after() {
        temporaryDirectory.deleteDir()
    }

    @Test
    void basicTest() {
        // test the normal circumstances (file exists and is allowed)
        testUsername = USER_NAME
        file         = EXISTING_FILE_NAME

        sendFileServiceMock.sendFile(isA(ServletContext),
                isA(HttpServletRequest), isA(HttpServletResponse),
                is(equalTo(targetFile)))

        play {
            controller.download()
        }

        assertThat response.status, is(200)
    }

    @Test
    void testNoPermission() {
        testUsername = OTHER_USER_NAME
        admin        = false

        play {
            controller.download()
        }

        assertThat response.status, is(403)
    }

    @Test
    void testAdminAlwaysHasPermission() {
        testUsername = ADMIN_NAME
        admin        = true
        file         = EXISTING_FILE_NAME

        sendFileServiceMock.sendFile(isA(ServletContext),
                isA(HttpServletRequest), isA(HttpServletResponse),
                is(equalTo(targetFile)))

        play {
            controller.download()
        }

        assertThat response.status, is(200)
    }

    @Test
    void testBadAnalysisName() {
        params.analysisName = 'not_a_valid_analysis_name'

        play {
            shouldFail InvalidRequestException, {
                controller.download()
            }
        }
    }

    @Test
    void testInexistingAnalysisName() {
        testUsername        = USER_NAME
        params.analysisName = ANALYSIS_NAME + '1'

        play {
            controller.download()
        }

        assertThat response.status, is(404)
    }

    @Test
    void testAccessToExternalFilesNotAllowed() {
        testUsername        = USER_NAME

        file = '../test'
        play {
            controller.download()
        }
        assertThat response.status, is(404)
    }

    @Test
    void testNonExistingFile() {
        testUsername = USER_NAME

        params.path = 'file_that_does_not_exist'

        play {
            controller.download()
        }

        assertThat response.status, is(404)
    }


}
