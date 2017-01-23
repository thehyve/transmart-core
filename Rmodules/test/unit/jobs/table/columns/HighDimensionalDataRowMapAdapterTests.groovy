package jobs.table.columns

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import jobs.table.MockTabularResultHelper
import org.gmock.WithGMock
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.highdim.AssayColumn

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@TestMixin(GrailsUnitTestMixin)
@WithGMock
class HighDimensionalDataRowMapAdapterTests {

    @Delegate
    MockTabularResultHelper mockTabularResultHelper

    @Before
    void before() {
        mockTabularResultHelper = new MockTabularResultHelper()
        mockTabularResultHelper.gMockController = $gmockController
    }

    @Test
    void basicTest() {
        List<AssayColumn> assays = createSampleAssays(2)
        DataRow row = createRowForAssays(assays, [1, 2], 'row1')

        play {
            def testee = new HighDimensionalDataRowMapAdapter(assays, row, 'prepend|')
            assertThat testee, allOf(
                    hasEntry(is('patient_1_subject_id'), hasEntry('prepend|row1', 1)),
                    hasEntry(is('patient_2_subject_id'), hasEntry('prepend|row1', 2)),)
        }
    }
}
