package jobs.steps.helpers

import com.google.common.collect.Lists
import grails.test.mixin.TestMixin
import jobs.UserParameters
import jobs.table.MissingValueAction
import jobs.table.Table
import org.junit.After
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn
import org.transmartproject.core.dataquery.clinical.PatientRow
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.querytool.QueryResult

import javax.annotation.Resource

import static groovy.util.GroovyAssert.shouldFail
import static jobs.steps.helpers.ConfiguratorTestsHelper.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@TestMixin(JobsIntegrationTestMixin)
class OptionalBinningColumnConfiguratorTests {

    public static final String COLUMN_HEADER = 'TEST COLUMN HEADER'

    @Autowired
    UserParameters params

    @Resource
    String jobName /* The job instance name */

    @Autowired
    @Qualifier('general')
    OptionalBinningColumnConfigurator testee

    @Autowired
    Table table

    @Delegate(interfaces = false)
    ConfiguratorTestsHelper configuratorTestsHelper = new ConfiguratorTestsHelper()

    void before() {
        initializeAsBean configuratorTestsHelper
        
        [testee, params, jobName, table].each {
            assertThat it, is(notNullValue())
        }
        assertThat params.@map, is(notNullValue())

        testee.header                = COLUMN_HEADER
        testee.projection            = Projection.DEFAULT_REAL_PROJECTION
        testee.keyForConceptPaths    = 'variable'
        testee.keyForDataType        = 'divVariableType'
        testee.keyForSearchKeywordId = 'divVariablePathway'

        BinningColumnConfigurator binningColumnConfigurator = testee.binningConfigurator

        binningColumnConfigurator.keyForDoBinning       = 'binning'
        binningColumnConfigurator.keyForManualBinning   = 'manualBinning'
        binningColumnConfigurator.keyForNumberOfBins    = 'numberOfBins'
        binningColumnConfigurator.keyForBinDistribution = 'binDistribution'
        binningColumnConfigurator.keyForBinRanges       = 'binRanges'
        // distinct from testee.keyForDataType!
        binningColumnConfigurator.keyForVariableType    = 'variableType'
    }

    @After
    void after() {
        table.backingMap.db.commit() //to check for non-serializable stuff
        table.close()
    }

    @Test
    void testHighDimensionalDataManualBinning() {
        params.@map.putAll([
                variable           : CONCEPT_PATH_HIGH_DIMENSION,
                divVariableType    : DATA_TYPE_NAME_HIGH_DIMENSION,
                divVariablePathway : SEARCH_KEYWORD_ID,

                binning            : 'TRUE',
                manualBinning      : 'TRUE',
                numberOfBins       : '2',
                binRanges          : 'bin1,0,45|bin2,45,65',
                variableType       : 'Continuous',
                result_instance_id1: RESULT_INSTANCE_ID1,
                result_instance_id2: RESULT_INSTANCE_ID2,
        ])


        /* interactions with tabular result */
        TabularResult<Double, AssayColumn> highDimResult = mock(TabularResult)
        def sampleAssays = createSampleAssays(2)
        highDimResult.indicesList.returns(sampleAssays)
        highDimResult.iterator().returns([
                createRowForAssays(sampleAssays, [10.0, 50.0], 'row label')].
                iterator())

        /* interactions with high dimension data type resource */
        createDataTypeResourceMock(highDimResult)

        play {
            testee.addColumn()

            table.buildTable()

            def res = table.result
            assertThat res, contains(
                    contains(equalTo("0 ≤ $COLUMN_HEADER ≤ 45" as String)),
                    contains(equalTo("45 < $COLUMN_HEADER ≤ 65" as String)))
        }
    }

    @Test
    void testHighDimensionalDataEvenSpacedBinning() {
        params.@map.putAll([
                variable           : CONCEPT_PATH_HIGH_DIMENSION,
                divVariableType    : DATA_TYPE_NAME_HIGH_DIMENSION,
                divVariablePathway : SEARCH_KEYWORD_ID,

                binning            : 'TRUE',
                manualBinning      : 'FALSE',
                numberOfBins       : '2',
                binDistribution    : 'ESB',

                result_instance_id1: RESULT_INSTANCE_ID1,
                result_instance_id2: RESULT_INSTANCE_ID2,
        ])

        /* interactions with tabular result */
        TabularResult<Double, AssayColumn> highDimResult = mock(TabularResult)
        def sampleAssays = createSampleAssays(3)
        highDimResult.indicesList.returns(sampleAssays)
        def halfPoint = (10.0 + 9001.0) / 2
        highDimResult.iterator().returns([
                createRowForAssays(sampleAssays, [10.0, halfPoint, 9001.0], 'row label')].
                iterator())

        /* interactions with high dimension data type resource */
        createDataTypeResourceMock(highDimResult)

        play {
            testee.addColumn()

            table.buildTable()

            def res = table.result
            assertThat res, contains(
                    contains(equalTo("10.0 ≤ $COLUMN_HEADER < $halfPoint" as String)),
                    contains(equalTo("$halfPoint ≤ $COLUMN_HEADER ≤ 9001.0" as String)),
                    contains(equalTo("$halfPoint ≤ $COLUMN_HEADER ≤ 9001.0" as String)))
        }
    }

    @Test
    void testHighDimensionalDataEvenDistributionBinning() {
        params.@map.putAll([
                variable           : CONCEPT_PATH_HIGH_DIMENSION,
                divVariableType    : DATA_TYPE_NAME_HIGH_DIMENSION,
                divVariablePathway : SEARCH_KEYWORD_ID,

                binning            : 'TRUE',
                manualBinning      : 'FALSE',
                numberOfBins       : '3',
                binDistribution    : 'EDP',

                result_instance_id1: RESULT_INSTANCE_ID1,
                result_instance_id2: RESULT_INSTANCE_ID2,
        ])

        /* interactions with tabular result */
        TabularResult<Double, AssayColumn> highDimResult = mock(TabularResult)
        def sampleAssays = createSampleAssays(10)
        highDimResult.indicesList.returns(sampleAssays)
        def values = [44, 47, 82, 79, 52, 95, 66, 75, 14, 5].collect { it as Double }
        highDimResult.iterator().returns([
                createRowForAssays(sampleAssays, values, 'row label')].
                iterator())

        /* interactions with high dimension data type resource */
        createDataTypeResourceMock(highDimResult)

        play {
            testee.addColumn()

            table.buildTable()

            def res = table.result

            //XXX: match to patients
            assertThat res, containsInAnyOrder(
                    (["5.0 ≤ $COLUMN_HEADER ≤ 44.0" as String] * 3 +
                            ["44.0 < $COLUMN_HEADER ≤ 66.0" as String] * 3 +
                            ["66.0 < $COLUMN_HEADER ≤ 95.0" as String] * 4).collect { equalTo([it]) })
        }
    }

    @Test
    void testEvenDistributionBinsFewData() {
        // less data points than bins
        params.@map.putAll([
                variable           : CONCEPT_PATH_CLINICAL,
                divVariableType    : DATA_TYPE_NAME_CLINICAL,

                binning            : 'TRUE',
                manualBinning      : 'FALSE',
                numberOfBins       : '3',
                binDistribution    : 'EDP',

                result_instance_id1: RESULT_INSTANCE_ID1,
                result_instance_id2: RESULT_INSTANCE_ID2,
        ])

        setupClinicalResult(2,
                createClinicalVariableColumns([CONCEPT_PATH_CLINICAL]),
                [12.0, 20.0])

        play {
            testee.addColumn()
            table.buildTable()
            def res = table.result

            assertThat res, containsInAnyOrder(
                    is(["12.0 ≤ $COLUMN_HEADER ≤ 12.0" as String]),
                    is(["12.0 < $COLUMN_HEADER ≤ 20.0" as String]),)
        }
    }

    @Test
    void testEvenDistributionBinsALotOfRepeats() {
        // less data points than bins
        params.@map.putAll([
                variable           : CONCEPT_PATH_CLINICAL,
                divVariableType    : DATA_TYPE_NAME_CLINICAL,

                binning            : 'TRUE',
                manualBinning      : 'FALSE',
                numberOfBins       : '3',
                binDistribution    : 'EDP',

                result_instance_id1: RESULT_INSTANCE_ID1,
                result_instance_id2: RESULT_INSTANCE_ID2,
        ])

        setupClinicalResult(6,
                createClinicalVariableColumns([CONCEPT_PATH_CLINICAL]),
                [20.0, 20.0, 20.0, 20.0, 30.0, 31.0])

        play {
            testee.addColumn()
            table.buildTable()
            def res = table.result

            /* There are only two effective bins here.
             * This is expected behavior.
             * See comment on calculateQuantileRanks */
            println res
            assertThat res, containsInAnyOrder(
                    (["20.0 ≤ $COLUMN_HEADER ≤ 20.0" as String] * 4 +
                    ["20.0 < $COLUMN_HEADER ≤ 31.0" as String] * 2).collect { equalTo([it]) })
        }
    }

    @Test
    void testClinicalDataContinuousManualBinning() {
        params.@map.putAll([
                variable           : CONCEPT_PATH_CLINICAL,
                divVariableType    : DATA_TYPE_NAME_CLINICAL,

                binning            : 'TRUE',
                manualBinning      : 'TRUE',
                numberOfBins       : '2',

                binRanges          : 'bin1,0,45|bin2,45,65',
                variableType       : 'Continuous',

                result_instance_id1: RESULT_INSTANCE_ID1,
                result_instance_id2: RESULT_INSTANCE_ID2,
        ])

        ClinicalVariableColumn column =
                createClinicalVariableColumns([CONCEPT_PATH_CLINICAL])[0]

        TabularResult<ClinicalVariableColumn, PatientRow> clinicalResult =
                mock(TabularResult)
        clinicalResult.iterator().returns(createPatientRows(
                3, [column], [0, 45, 50]).iterator())


        ArrayList<QueryResult> queryResults = mockQueryResults()

        clinicalDataResourceMock.retrieveData(queryResults, [column]).returns(clinicalResult)

        play {
            testee.addColumn()

            table.buildTable()

            def res = table.result
            assertThat res, contains(
                    contains(equalTo("0 ≤ $COLUMN_HEADER ≤ 45" as String)),
                    contains(equalTo("0 ≤ $COLUMN_HEADER ≤ 45" as String)),
                    contains(equalTo("45 < $COLUMN_HEADER ≤ 65" as String)))
        }
    }

    @Test
    void testClinicalDataCategoricalBinning() {
        params.@map.putAll([
                variable           : BUNDLE_OF_CLINICAL_CONCEPT_PATH.join('|'),
                divVariableType    : DATA_TYPE_NAME_CLINICAL,

                binning            : 'TRUE',
                manualBinning      : 'TRUE',
                numberOfBins       : '2',

                binRanges          : ("bin1<>${BUNDLE_OF_CLINICAL_CONCEPT_PATH[0..1].join('<>')}|" +
                                     "bin2<>${BUNDLE_OF_CLINICAL_CONCEPT_PATH[2]}") as String,
                variableType       : 'Categorical',

                result_instance_id1: RESULT_INSTANCE_ID1,
                result_instance_id2: RESULT_INSTANCE_ID2,
        ])

        List<ClinicalVariableColumn> columns =
                createClinicalVariableColumns BUNDLE_OF_CLINICAL_CONCEPT_PATH, true

        List<String> valuesForColumns = (1..3).collect { "value for col$it".toString() }

        TabularResult<ClinicalVariableColumn, PatientRow> clinicalResult =
                mock(TabularResult)
        clinicalResult.iterator().returns(createPatientRows(4, columns,
                ['',                 '',                  valuesForColumns[2],
                 '',                 valuesForColumns[1], '',
                valuesForColumns[0], '',                  '',
                '',                  valuesForColumns[1], '',],
                true /* relaxed */).iterator())
        clinicalResult.close().stub()


        ArrayList<QueryResult> queryResults = mockQueryResults()

        clinicalDataResourceMock.retrieveData(
                queryResults,
                containsInAnyOrder(columns.collect { is it })).returns(clinicalResult)

        play {
            testee.addColumn()

            table.buildTable()

            def res = table.result
            assertThat res, containsInAnyOrder(
                    [is(['bin1'])] * 3 +
                            is(['bin2']))
        }
    }

    @Test
    void testMissingValueAction() {
        params.@map.putAll([
                variable           : CONCEPT_PATH_CLINICAL,
                divVariableType    : DATA_TYPE_NAME_CLINICAL,

                binning            : 'TRUE',
                manualBinning      : 'TRUE',
                numberOfBins       : '2',

                binRanges          : 'bin1,0,45|bin2,45,65',
                variableType       : 'Continuous',

                result_instance_id1: RESULT_INSTANCE_ID1,
                result_instance_id2: RESULT_INSTANCE_ID2,
        ])

        /* clinical variables */
        ClinicalVariableColumn clinicalVariable =
                createClinicalVariableColumns([CONCEPT_PATH_CLINICAL])[0]

        /* result set */
        TabularResult<ClinicalVariableColumn, PatientRow> clinicalResult =
                mock(TabularResult)
        clinicalResult.iterator().returns(createPatientRows(
                3, [clinicalVariable],
                [ 0, 66, 50]).iterator())
        // 66 is outside any bin, so a null will be returned by the binning column decorator
        clinicalDataResourceMock.retrieveData(mockQueryResults(), [clinicalVariable]).returns(clinicalResult)

        testee.missingValueAction =
                new MissingValueAction.ConstantReplacementMissingValueAction(replacement: 'foo')

        // second column to force the skipped primary key to be in the result
        // otherise the row with 66 would be just skipped
        def secondColumn = new StubColumn(
                header: 'STUB',
                data: [(createPatientRowLabels(2)[1]): 'bar'])
        secondColumn.missingValueAction =
                new MissingValueAction.ConstantReplacementMissingValueAction(replacement: '')

        play {
            testee.addColumn()
            table.addColumn(secondColumn, [] as Set)

            table.buildTable()
            table.backingMap

            List res = Lists.newArrayList table.result
            assertThat res, containsInAnyOrder(
                    contains(equalTo("0 ≤ $COLUMN_HEADER ≤ 45" as String), equalTo('')),
                    contains(equalTo("45 < $COLUMN_HEADER ≤ 65" as String), equalTo('')),
                    contains(equalTo('foo'), equalTo('bar')))
        }
    }

    @Test
    void testHighDimensionMultiRow() {
        params.@map.putAll([
                variable           : CONCEPT_PATH_HIGH_DIMENSION,
                divVariableType    : DATA_TYPE_NAME_HIGH_DIMENSION,
                divVariablePathway : SEARCH_KEYWORD_ID,

                binning            : 'FALSE',
                result_instance_id1: RESULT_INSTANCE_ID1,
                result_instance_id2: RESULT_INSTANCE_ID2,
        ])

        /* interactions with tabular result */
        TabularResult<Double, AssayColumn> highDimResult = mock(TabularResult)
        def sampleAssays = createSampleAssays(2)
        highDimResult.indicesList.returns(sampleAssays)
        highDimResult.iterator().returns([
                createRowForAssays(sampleAssays, [10.0, 50.0], 'row label 1'),
                createRowForAssays(sampleAssays, [20.0, 100.0], 'row label 2')].
                iterator())

        /* interactions with high dimension data type resource */
        createDataTypeResourceMock(highDimResult)

        play {
            testee.multiRow = true
            testee.forceNumericBinning = false
            testee.addColumn()

            table.buildTable()

            def res = table.result
            assertThat res, containsInAnyOrder(
                    contains(allOf(
                            hasEntry(is('row label 1'), is(10.0 as BigDecimal)),
                            hasEntry(is('row label 2'), is(20.0 as BigDecimal)))),
                    contains(allOf(
                            hasEntry(is('row label 1'), is(50.0 as BigDecimal)),
                            hasEntry(is('row label 2'), is(100.0 as BigDecimal)))))
        }
    }

    @Test
    void testHighDimensionMultiRowManualBinning() {
        params.@map.putAll([
                variable           : CONCEPT_PATH_HIGH_DIMENSION,
                divVariableType    : DATA_TYPE_NAME_HIGH_DIMENSION,
                divVariablePathway : SEARCH_KEYWORD_ID,

                binning            : 'TRUE',
                manualBinning      : 'TRUE',
                numberOfBins       : '2',
                binRanges          : 'bin1,0,45|bin2,45,65',

                result_instance_id1: RESULT_INSTANCE_ID1,
                result_instance_id2: RESULT_INSTANCE_ID2,
        ])

        /* interactions with tabular result */
        TabularResult<Double, AssayColumn> highDimResult = mock(TabularResult)
        def sampleAssays = createSampleAssays(2)
        highDimResult.indicesList.returns(sampleAssays)
        highDimResult.iterator().returns([
                createRowForAssays(sampleAssays, [10.0, 50.0], 'row label 1'),
                createRowForAssays(sampleAssays, [45.0, 70.0 /* out of bounds */], 'row label 2')].
                iterator())

        /* interactions with high dimension data type resource */
        createDataTypeResourceMock(highDimResult)

        play {
            testee.multiRow = true
            testee.addColumn()

            table.buildTable()

            def res = table.result
            assertThat res, containsInAnyOrder(
                    contains(allOf(
                            hasEntry(is('row label 1'), is("0 ≤ $COLUMN_HEADER ≤ 45" as String)),
                            hasEntry(is('row label 2'), is("0 ≤ $COLUMN_HEADER ≤ 45" as String)))),
                    contains(allOf(
                            hasEntry(is('row label 1'), is("45 < $COLUMN_HEADER ≤ 65" as String)))))
        }
    }

    @Test
    void testHighDimensionMultiRowEvenSpacedBinning() {
        params.@map.putAll([
                variable           : CONCEPT_PATH_HIGH_DIMENSION,
                divVariableType    : DATA_TYPE_NAME_HIGH_DIMENSION,
                divVariablePathway : SEARCH_KEYWORD_ID,

                binning            : 'TRUE',
                manualBinning      : 'FALSE',
                numberOfBins       : '2',
                binDistribution    : 'ESB',

                result_instance_id1: RESULT_INSTANCE_ID1,
                result_instance_id2: RESULT_INSTANCE_ID2,
        ])

        /* interactions with tabular result */
        TabularResult<Double, AssayColumn> highDimResult = mock(TabularResult)
        def sampleAssays = createSampleAssays(3)
        highDimResult.indicesList.returns(sampleAssays)
        highDimResult.iterator().returns([
                createRowForAssays(sampleAssays, [10.0, null, 60.0], 'row label 1'),
                createRowForAssays(sampleAssays, [0.0, 50.0, 100.0], 'row label 2')].
                iterator())

        /* interactions with high dimension data type resource */
        createDataTypeResourceMock(highDimResult)

        play {
            testee.multiRow = true
            testee.addColumn()

            table.buildTable()

            def res = table.result
            assertThat res, containsInAnyOrder(
                    contains(allOf(
                            hasEntry(is('row label 1'), is("10.0 ≤ $COLUMN_HEADER < 35.0" as String)),
                            hasEntry(is('row label 2'), is("0.0 ≤ $COLUMN_HEADER < 50.0" as String)))),
                    contains(allOf(
                            hasEntry(is('row label 2'), is("50.0 ≤ $COLUMN_HEADER ≤ 100.0" as String)))),
                    contains(allOf(
                            hasEntry(is('row label 1'), is("35.0 ≤ $COLUMN_HEADER ≤ 60.0" as String)),
                            hasEntry(is('row label 2'), is("50.0 ≤ $COLUMN_HEADER ≤ 100.0" as String)))))
        }
    }

    @Test
    void testHighDimensionMultiRowEvenDistributionBinning() {
        params.@map.putAll([
                variable           : CONCEPT_PATH_HIGH_DIMENSION,
                divVariableType    : DATA_TYPE_NAME_HIGH_DIMENSION,
                divVariablePathway : SEARCH_KEYWORD_ID,

                binning            : 'TRUE',
                manualBinning      : 'FALSE',
                numberOfBins       : '2',
                binDistribution    : 'EDP',

                result_instance_id1: RESULT_INSTANCE_ID1,
                result_instance_id2: RESULT_INSTANCE_ID2,
        ])

        /* interactions with tabular result */
        TabularResult<Double, AssayColumn> highDimResult = mock(TabularResult)
        def sampleAssays = createSampleAssays(4)
        highDimResult.indicesList.returns(sampleAssays)
        highDimResult.iterator().returns([
                createRowForAssays(sampleAssays, [10.0, null, 60.0, 61.0], 'row label 1'),
                createRowForAssays(sampleAssays, [0.0, 10.0, 10.0, 20.0], 'row label 2')].
                iterator())

        /* interactions with high dimension data type resource */
        createDataTypeResourceMock(highDimResult)

        play {
            testee.multiRow = true
            testee.addColumn()

            table.buildTable()

            def res = table.result
            assertThat res, containsInAnyOrder(
                    contains(allOf(
                            hasEntry(is('row label 1'), is("10.0 ≤ $COLUMN_HEADER ≤ 10.0" as String)),
                            hasEntry(is('row label 2'), is("0.0 ≤ $COLUMN_HEADER ≤ 10.0" as String)))),
                    contains(allOf(
                            hasEntry(is('row label 2'), is("0.0 ≤ $COLUMN_HEADER ≤ 10.0" as String)))),
                    contains(allOf(
                            hasEntry(is('row label 1'), is("10.0 < $COLUMN_HEADER ≤ 61.0" as String)),
                            hasEntry(is('row label 2'), is("0.0 ≤ $COLUMN_HEADER ≤ 10.0" as String)))),
                    contains(allOf(
                            hasEntry(is('row label 1'), is("10.0 < $COLUMN_HEADER ≤ 61.0" as String)),
                            hasEntry(is('row label 2'), is("10.0 < $COLUMN_HEADER ≤ 20.0" as String)))))
        }
    }

    @Test
    void testCategoricalVariableUsedAsNumeric() {
        params.@map.putAll([
                variable           : CONCEPT_PATH_CLINICAL,
                divVariableType    : DATA_TYPE_NAME_CLINICAL,

                binning            : 'FALSE',

                result_instance_id1: RESULT_INSTANCE_ID1,
                result_instance_id2: RESULT_INSTANCE_ID2,
        ])


        /* clinical variables */
        List<ClinicalVariableColumn> clinicalVariables =
                createClinicalVariableColumns([CONCEPT_PATH_CLINICAL])
        setupClinicalResult(3, clinicalVariables, [null, 'foobar', null])

        testee.forceNumericBinning = false

        assertThat shouldFail(InvalidArgumentsException, {
            play {
                testee.addColumn()

                table.buildTable()
                Lists.newArrayList table.result
            }
        }), hasProperty('message', containsString('Got non-numerical value'))
    }
}
