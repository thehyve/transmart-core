package com.recomdata.transmart.data.association

import grails.test.mixin.TestFor
import org.junit.Before
import org.junit.Test
import org.gmock.WithGMock

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*


@TestFor(RModulesOutputRenderService)
@WithGMock
class RModulesOutputRenderServiceTests {

    private static final String USER_NAME = 'user'
    private static final String FILE_CONTENTS = 'file contents\n'
    private static final String ANALYSIS_NAME = "$USER_NAME-Analysis-100"
    private static final String WORKING_DIRECTORY = 'workingDirectory'

    File temporaryDirectory
    File analysisDirectory
    File workingDirectory

    @Before
    void before() {
        temporaryDirectory = File.createTempDir('analysis_file_test', '')
        grailsApplication.config.RModules.tempFolderDirectory = temporaryDirectory.absolutePath

        analysisDirectory = new File(temporaryDirectory, ANALYSIS_NAME)
        analysisDirectory.mkdir()
        workingDirectory = new File(analysisDirectory, WORKING_DIRECTORY)
        workingDirectory.mkdir()

        service.zipService = mock()
        service.asyncJobService = mock()

        createDummyFile(workingDirectory, "Heatmap&*.png")
        createDummyFile(workingDirectory, "Heatmap.svg")
        createDummyFile(workingDirectory, "jobInfo.txt")
        createDummyFile(workingDirectory, "outputfile.txt")
        createDummyFile(workingDirectory, "request.json")
    }

    void createDummyFile(File directory, String fileName) {
        File dummyFile = new File(directory, fileName)
        dummyFile << FILE_CONTENTS
    }

    @Test
    void testInitializeAttributes() {
        def ArrayList<String> imageLinks = new ArrayList<String>()
        service.initializeAttributes(ANALYSIS_NAME, "Analysis", imageLinks)

        assertTrue "File not found: Heatmap__.png", new File(workingDirectory, "Heatmap__.png").exists()
        assertTrue "File not found: Heatmap.svg", new File(workingDirectory, "Heatmap.svg").exists()
        assertTrue "File not found: jobInfo.txt", new File(workingDirectory, "jobInfo.txt").exists()
        assertTrue "File not found: outputfile.txt", new File(workingDirectory, "outputfile.txt").exists()
        assertTrue "File not found: request.json", new File(workingDirectory, "request.json").exists()
        assertThat imageLinks, contains("/analysisFiles/user-Analysis-100/workingDirectory/Heatmap__.png")
        assertThat service.zipLink.toString(), equalTo("/analysisFiles/user-Analysis-100/zippedData.zip")
    }

}
