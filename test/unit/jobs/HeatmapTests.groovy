package jobs

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.gmock.WithGMock
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test

@TestMixin(GrailsUnitTestMixin)
@WithGMock
class HeatmapTests {

    Heatmap testee

    @Before
    void setUp() {
        testee = new Heatmap()
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

    /*
    void writesParameterFileTest() {}

    void fetchesResultsTest() {}

    void writesResultsTest() {}

    void runsAnalysisTest() {}
    */
}
