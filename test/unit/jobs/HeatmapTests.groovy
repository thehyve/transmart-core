package jobs

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import static org.easymock.EasyMock.*;

@TestMixin(GrailsUnitTestMixin)
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
    void createConceptKeyFromTest() {
        String prefix = Heatmap.createConceptKeyFrom(
                '\\Public Studies\\GSE8581\\MRNA\\Biomarker Data\\Affymetrix Human Genome U133A 2.0 Array\\Lung\\'
        )
        assert prefix == '\\\\Public Studies\\Public Studies\\GSE8581\\MRNA\\Biomarker Data\\Affymetrix Human Genome U133A 2.0 Array\\Lung\\'
    }

    @Test
    void throwsExceptionOnMalformattedJobName() {
        def context = mockedJobExecutionContext([jobName: 'contains_underscores'])
        println(context.jobDetail)
        shouldFail(JobExecutionException) {
            heatmap.execute(context)
        }
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

    JobExecutionContext mockedJobExecutionContext(Map data) {
        JobDetail jobDetail = createMock(JobDetail.class)
        expect(jobDetail.getJobDataMap()).andReturn(new JobDataMap(data)).anyTimes()
        replay(jobDetail)

        JobExecutionContext context = createMock(JobExecutionContext.class)
        expect(context.getJobDetail()).andReturn(jobDetail).anyTimes()
        replay(context)

        context
    }

    /*
    void writesParameterFileTest() {}

    void fetchesResultsTest() {}

    void writesResultsTest() {}

    void runsAnalysisTest() {}
    */
}
