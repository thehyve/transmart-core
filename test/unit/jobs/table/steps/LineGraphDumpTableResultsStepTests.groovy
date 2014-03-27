package jobs.table.steps

import au.com.bytecode.opencsv.CSVReader
import com.google.common.base.Function
import com.google.common.collect.Lists
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import jobs.steps.LineGraphDumpTableResultsStep
import jobs.table.Table
import org.gmock.WithGMock
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.contains
import static org.hamcrest.Matchers.is

@TestMixin(GrailsUnitTestMixin)
@WithGMock
class LineGraphDumpTableResultsStepTests {

    private LineGraphDumpTableResultsStep testee = new LineGraphDumpTableResultsStep()

    Table mockTable

    File temporaryDirectory = File.createTempDir()

    @Before
    void before() {
        mockTable = mock(Table)
        mockTable.close()

        testee.table = mockTable
        testee.temporaryDirectory = temporaryDirectory
    }

    @After
    void after() {
        temporaryDirectory.deleteDir()
    }

    List<List<String>> readOutputFile() {
        def reader = new File(temporaryDirectory, 'outputfile.txt').newReader('UTF-8')
        try {
            def csvReader = new CSVReader(reader, '\t' as char)
            Lists.transform(csvReader.readAll(), { it as List } as Function)
        } finally {
            reader.close()
        }
    }

    @Test
    void testOneMapColumn() {
        def data = [
                ['patient 1', ['a|1': 1.0, 'b|1': 2.0], 'group for 1'],
                ['patient 2', ['c|2': 3.0, 'd|2': 4.0], 'group for 2'],
        ]
        mockTable.headers.returns(['PATIENT', 'VALUE', 'GROUP_VAR'])
        mockTable.result.returns data

        play {
            testee.execute()
        }

        def res = readOutputFile()
        assertThat res, contains(
                is(['PATIENT', 'VALUE', 'GROUP', 'PLOT_GROUP', 'GROUP_VAR']),
                is(['patient 1', '1.0', 'a', '1', 'group for 1']),
                is(['patient 1', '2.0', 'b', '1', 'group for 1']),
                is(['patient 2', '3.0', 'c', '2', 'group for 2']),
                is(['patient 2', '4.0', 'd', '2', 'group for 2']),
        )
    }

    @Test
    void testTwoMapColumn() {
        def data = [
                ['patient 1', ['a|1': 1.0, 'b|1': 2.0], [probe1: '1 < X < 2', probe2: '2 < X < 3']],
        ]
        mockTable.headers.returns(['PATIENT', 'VALUE', 'GROUP_VAR'])
        mockTable.result.returns data

        play {
            testee.execute()
        }

        def res = readOutputFile()
        assertThat res, contains(
                is(['PATIENT',   'VALUE', 'GROUP', 'PLOT_GROUP', 'GROUP_VAR']),
                is(['patient 1', '1.0',   'a',     '1_probe1',   '1 < X < 2']),
                is(['patient 1', '1.0',   'a',     '1_probe2',   '2 < X < 3']),
                is(['patient 1', '2.0',   'b',     '1_probe1',   '1 < X < 2']),
                is(['patient 1', '2.0',   'b',     '1_probe2',   '2 < X < 3']),
        )
    }

}
