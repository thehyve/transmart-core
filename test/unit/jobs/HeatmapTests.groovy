package jobs

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.gmock.WithGMock
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.quartz.JobExecutionContext
import org.transmartproject.core.dataquery.TabularResult

@TestMixin(GrailsUnitTestMixin)
@WithGMock
class HeatmapTests {

    Heatmap heatmap


    @Before
    void setUp() {
        heatmap = new Heatmap()
        // heatmap.temporaryDirectory = "/tmp/unit_tests"
    }

    @After
    void tearDown() {
        // new File(heatmap.temporaryDirectory).deleteDir()
    }

    @Test
    void scheduleJobTest() {
        def scheduler = mock()
        scheduler.scheduleJob(Matchers.anything(), Matchers.anything()).once()

        Heatmap.metaClass.static.getQuartzScheduler = { -> scheduler }

        play {
            Heatmap.scheduleJob([jobName: 'boar'])
        }
    }

    @Test
    void setupTemporaryDirectoryTest() {
        heatmap.metaClass.writeParametersFile = { }
        heatmap.metaClass.fetchResults = { }
        heatmap.metaClass.writeData = { TabularResult a, File b -> null }
        heatmap.metaClass.runAnalysis = { }

        JobExecutionContext context = mock(JobExecutionContext)
        def jobDetailMap = mock()
        jobDetailMap.jobDataMap.returns([jobName: 'foo_bar_baz']).stub()
        context.jobDetail.returns(jobDetailMap).stub()

        play {
            heatmap.execute(context)
            assert new File(new File(heatmap.temporaryDirectory, 'foo_bar_baz'), 'workingDirectory').exists()
        }
    }

    @Test
    void writesParameterFileTest() {

    }

    @Test
    void processTemplatesSuccessTest() {
        String source = 'source(\'$pluginDirectory/Heatmap/HeatmapLoader.R\')'
        Map vars = [:]
        vars.pluginDirectory = 'sometest'
        String finalSource = heatmap.processTemplates(source, vars)
        assert finalSource == 'source(\'sometest/Heatmap/HeatmapLoader.R\')'
    }

    @Test
    void processTemplatesMultipleVarsSuccessTest() {
        String createHeatmap = '''
            Heatmap.loader(
                            input.filename = \'outputfile\',
                            imageWidth     = as.integer(\'$txtImageWidth\'),
                            imageHeight    = as.integer(\'$txtImageHeight\'),
                            pointsize      = as.integer(\'$txtImagePointsize\'),
                            maxDrawNumber  = as.integer(\'$txtMaxDrawNumber\'))
            '''

        Map vars = [:]
        vars.txtImageWidth = 'imagewidth'
        vars.txtImageHeight = 'imageheight'
        vars.txtImagePointsize = 'imagepointsize'
        vars.txtMaxDrawNumber = 'imagemaxdrawnumber'
        String finalSource = heatmap.processTemplates(createHeatmap, vars)
        assert finalSource == """
            Heatmap.loader(
                            input.filename = 'outputfile',
                            imageWidth     = as.integer('imagewidth'),
                            imageHeight    = as.integer('imageheight'),
                            pointsize      = as.integer('imagepointsize'),
                            maxDrawNumber  = as.integer('imagemaxdrawnumber'))
            """
    }

    /*
    void fetchesResultsTest() {}

    void writesResultsTest() {}

    void runsAnalysisTest() {}
    */
}
