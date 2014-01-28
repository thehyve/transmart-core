package jobs.table.steps.helpers

import com.google.common.collect.Lists
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import jobs.steps.helpers.CompositeTabularResult
import org.gmock.WithGMock
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.exceptions.InvalidArgumentsException

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@TestMixin(GrailsUnitTestMixin)
@WithGMock
class CompositeTabularResultTests {

    public static final String CONCEPT_PATH_1 = '\\concept path\\1\\'
    public static final String CONCEPT_PATH_2 = '\\concept path\\2\\'

    @Delegate
    MockTabularResultHelper mockTabularResultHelper

    CompositeTabularResult testee

    @Before
    void before() {
        mockTabularResultHelper = new MockTabularResultHelper()
        mockTabularResultHelper.gMockController = $gmockController
    }

    @Test
    void basicTest() {
        List<AssayColumn> assays = createSampleAssays(4)

        TabularResult result1 = createMockTabularResult(
                columnsLabel: 'cols label 1',
                rowsLabel:    'rows label 1',
                assays:       assays[0..2],
                data:         [ row1: [1, 2, 3],
                                row2: [4, 5, 6]])

        TabularResult result2 = createMockTabularResult(
                columnsLabel: 'cols label 2',
                rowsLabel:    'rows label 2',
                assays:       assays[1..3],
                data:         [ row1: [7, 8, 9],
                                row2: [10, 11, 12]])

        play {
            testee = new CompositeTabularResult(
                    results: [ (CONCEPT_PATH_1): result1,
                            (CONCEPT_PATH_2): result2 ])

            assertThat testee, allOf(
                    hasProperty('columnsDimensionLabel', equalTo('cols label 1/cols label 2')),
                    hasProperty('rowsDimensionLabel', equalTo('rows label 1/rows label 2')))

            // assays are sorted by patientInTrialId
            assertThat testee, hasProperty('indicesList',
                    contains(assays[1..2].collect { sameInstance it }))

            List results = Lists.newArrayList(testee.getRows())
            assertThat results, contains(
                    allOf(
                            isA(DataRow),
                            hasProperty('label', equalTo("$CONCEPT_PATH_1|row1" as String)),
                            contains([2, 3].collect { is it })
                    ),
                    allOf(
                            hasProperty('label', equalTo("$CONCEPT_PATH_1|row2" as String)),
                            contains([5, 6].collect { is it })
                    ),
                    allOf(
                            hasProperty('label', equalTo("$CONCEPT_PATH_2|row1" as String)),
                            contains([7, 8].collect { is it })
                    ),
                    allOf(
                            hasProperty('label', equalTo("$CONCEPT_PATH_2|row2" as String)),
                            contains([10, 11].collect { is it })
                    ))
        }
    }

    @Test
    void testNoAssaysInCommon() {
        List<AssayColumn> assays = createSampleAssays(2)

        TabularResult result1 = createMockTabularResult(
                assays: [ assays[0] ],
                data:   [ row1: [1] ])

        TabularResult result2 = createMockTabularResult(
                assays: [ assays[1] ],
                data:   [ row1: [2] ])

        play {
            assertThat shouldFail(InvalidArgumentsException, {
                testee = new CompositeTabularResult(
                        results: [ (CONCEPT_PATH_1): result1,
                                (CONCEPT_PATH_2): result2 ])
                Lists.newArrayList(testee.getRows())
            }), containsString('intersection of the assays of the 2 result sets is empty')
        }
    }

    private TabularResult<AssayColumn, Number> createMockTabularResult(Map params) {
        List<AssayColumn> sampleAssays        = params.assays
        Map<String, List<Number>> labelToData = params.data
        String columnsDimensionLabel          = params.columnsLabel
        String rowsDimensionLabel             = params.rowsLabel

        TabularResult highDimResult = mock TabularResult
        highDimResult.indicesList.returns(sampleAssays).atLeastOnce()
        highDimResult.getRows().returns(
                labelToData.collect { String label, List<Number> data ->
                    createRowForAssays(sampleAssays, data, label)
                }.iterator())

        if (columnsDimensionLabel) {
            highDimResult.columnsDimensionLabel.returns columnsDimensionLabel
        }
        if (rowsDimensionLabel) {
            highDimResult.rowsDimensionLabel.returns rowsDimensionLabel
        }

        highDimResult
    }

}
