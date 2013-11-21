package jobs

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.gmock.GMockTestCase
import org.junit.Before
import org.junit.Test

@TestMixin(GrailsUnitTestMixin)
class HeatmapTests {

    Heatmap testee

    @Before
    void setUp() {
        testee = new Heatmap()
    }


    @Test
    void scheduleJobTest() {
        def scheduler = mock()
        scheduler.scheduleJob().once()
        Heatmap heatmap = mock(Heatmap)
        heatmap.static.getQuartzScheduler().returns(scheduler)
        assert 1 == 1
        play {
            heatmap.scheduleJob([:])
        }
    }

    @Test
    void writesParameterFileTest() {

    }

    @Test
    void fetchesResultsTest() {}

    @Test
    void writesResultsTest() {}

    @Test
    void runsAnalysisTest() {}
}
