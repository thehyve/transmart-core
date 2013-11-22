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
        heatmap.temporaryDirectory = "/tmp/unit_tests"
    }

    @After
    void tearDown() {
        new File(heatmap.temporaryDirectory).deleteDir()
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

    /*
    void fetchesResultsTest() {}

    void writesResultsTest() {}

    void runsAnalysisTest() {}
    */
}
