package jobs.steps.helpers

import com.google.common.collect.ImmutableTable
import com.google.common.collect.Lists
import grails.test.mixin.TestMixin
import jobs.UserParameters
import jobs.table.MissingValueAction
import jobs.table.Table
import jobs.table.columns.AbstractColumn
import org.gmock.GMockController
import org.junit.After
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.clinical.ClinicalDataResource
import org.transmartproject.core.dataquery.clinical.ClinicalVariable
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn
import org.transmartproject.core.dataquery.clinical.PatientRow
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.querytool.QueriesResource
import org.transmartproject.core.querytool.QueryResult

import javax.annotation.Resource

import static jobs.steps.OpenHighDimensionalDataStep.createConceptKeyFrom
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@TestMixin(JobsIntegrationTestMixin)
class ColumnConfiguratorTests {

    public static final String COLUMN_HEADER = 'TEST COLUMN HEADER'

    public static final String SEARCH_KEYWORD_ID = '88888'
    public static final String DATA_TYPE_NAME_HIGH_DIMENSION = 'mrna'
    public static final String DATA_TYPE_NAME_CLINICAL = 'CLINICAL'
    public static final String RESULT_INSTANCE_ID1 = '77'
    public static final String RESULT_INSTANCE_ID2 = '78'
    public static final String CONCEPT_PATH_HIGH_DIMENSION = '\\bogus\\highdim\\variable\\'

    public static final String CONCEPT_PATH_CLINICAL = '\\bogus\\clinical\\variable\\'

    public static final List<String> BUNDLE_OF_CLINICAL_CONCEPT_PATH = [
            '\\bogus\\clinical\\variable\\1',
            '\\bogus\\clinical\\variable\\2',
            '\\bogus\\clinical\\variable\\3',
    ]

    @Autowired
    UserParameters params

    @Resource
    String jobName /* The job instance name */

    @Autowired
    @Qualifier('general')
    OptionalBinningColumnConfigurator testee

    @Autowired
    Table table


    /* mocked core-db services */
    @Autowired
    HighDimensionResource highDimensionResourceMock

    @Autowired
    ClinicalDataResource clinicalDataResourceMock

    @Autowired
    QueriesResource queriesResourceMock


    @Autowired
    @Delegate(interfaces = false)
    GMockController gMockController

    void before() {
        [testee, params, jobName, table].each {
            assertThat it, is(notNullValue())
        }
        assertThat params.@map, is(notNullValue())

        testee.columnHeader          = COLUMN_HEADER
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

        ClinicalVariableColumn column = mock(ClinicalVariableColumn)
        clinicalDataResourceMock.createClinicalVariable(
                ClinicalVariable.TERMINAL_CONCEPT_VARIABLE,
                concept_path: CONCEPT_PATH_CLINICAL).returns(column)

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

        List<ClinicalVariableColumn> columns = [mock(ClinicalVariableColumn),
                mock(ClinicalVariableColumn), mock(ClinicalVariableColumn)]
        dot(columns, BUNDLE_OF_CLINICAL_CONCEPT_PATH) { ClinicalVariableColumn col, String path ->
            col.label.returns(path).atLeastOnce()

            clinicalDataResourceMock.createClinicalVariable(
                    ClinicalVariable.TERMINAL_CONCEPT_VARIABLE,
                    concept_path: path).returns(col)
        }

        List<String> valuesForColumns = (1..3).collect { "value for col$it".toString() }

        TabularResult<ClinicalVariableColumn, PatientRow> clinicalResult =
                mock(TabularResult)
        clinicalResult.iterator().returns(createPatientRows(4, columns,
                ['', '', valuesForColumns[2],
                 '', valuesForColumns[1], '',
                valuesForColumns[0], '', '',
                '', valuesForColumns[1], '',],
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
        ClinicalVariableColumn clinicalVariable = mock(ClinicalVariableColumn)
        clinicalDataResourceMock.createClinicalVariable(
                ClinicalVariable.TERMINAL_CONCEPT_VARIABLE,
                concept_path: CONCEPT_PATH_CLINICAL).returns(clinicalVariable)

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
        def secondColumn = new StubColumn(data: [
                (createPatientRowLabels(2)[1]): 'bar'
        ])

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

    static class StubColumn extends AbstractColumn {

        Map<String, String> data

        @Override
        void onReadRow(String dataSourceName, Object row) {}

        @Override
        Map<String, String> consumeResultingTableRows() {
            try {
                return data
            } finally {
                data = null
            }
        }
    }

    private ArrayList<QueryResult> mockQueryResults() {
        def queryResults = [mock(QueryResult), mock(QueryResult)]
        queriesResourceMock.getQueryResultFromId(RESULT_INSTANCE_ID1 as int).returns(queryResults[0])
        queriesResourceMock.getQueryResultFromId(RESULT_INSTANCE_ID2 as int).returns(queryResults[1])
        queryResults
    }

    private void createDataTypeResourceMock(TabularResult<Double, AssayColumn> highDimResult) {
        HighDimensionDataTypeResource dataTypeResourceMock =
                mock(HighDimensionDataTypeResource)
        ordered {
            highDimensionResourceMock.getSubResourceForType(DATA_TYPE_NAME_HIGH_DIMENSION).
                    returns(dataTypeResourceMock)
            unordered {
                dataTypeResourceMock.createProjection(
                        [:], Projection.DEFAULT_REAL_PROJECTION).returns(mock(Projection))
                dataTypeResourceMock.createAssayConstraint(AssayConstraint.DISJUNCTION_CONSTRAINT,
                        subconstraints: [
                                (AssayConstraint.PATIENT_SET_CONSTRAINT): [
                                        [result_instance_id: RESULT_INSTANCE_ID1 as Long],
                                        [result_instance_id: RESULT_INSTANCE_ID2 as Long],
                                ]]).returns(mock(AssayConstraint))
                dataTypeResourceMock.createAssayConstraint(AssayConstraint.ONTOLOGY_TERM_CONSTRAINT,
                        concept_key: createConceptKeyFrom(CONCEPT_PATH_HIGH_DIMENSION)).returns(mock(AssayConstraint))
                dataTypeResourceMock.createDataConstraint(DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT,
                        keyword_ids: [SEARCH_KEYWORD_ID]).returns(mock(DataConstraint))
            }
            dataTypeResourceMock.retrieveData(
                    allOf(hasSize(2), everyItem(isA(AssayConstraint))),
                    everyItem(isA(DataConstraint)), isA(Projection)).returns(highDimResult)
        }
    }

    private List<AssayColumn> createSampleAssays(int n) {
        (1..n).collect {
            createMockAssay("patient_${it}_subject_id",
                    "sample_code_${it}")
        }
    }

    private DataRow createRowForAssays(List<AssayColumn> assays,
                                              List<Double> data,
                                              String label) {
        createMockRow(
                dot(assays, data, {a, b -> [ a, b ]})
                        .collectEntries(Closure.IDENTITY),
                label)
    }

    private static List dot(List list1, List list2, function) {
        def res = []
        for (int i = 0; i < list1.size(); i++) {
            res << function(list1[i], list2[i])
        }
        res
    }

    private List<Patient> createPatients(int n) {
        (1..n).collect {
            Patient p = mock(Patient)
            p.inTrialId.returns("subject id #$it".toString()).atLeastOnce()
            p
        }
    }

    private List<String> createPatientRowLabels(int n) {
        (1..n).collect {
            "patient #$it" as String
        }
    }

    private List<PatientRow> createPatientRows(int n,
                                   List<ClinicalVariableColumn> columns,
                                   List<String> values,
                                   boolean relaxed = false) {
        def builder = ImmutableTable.builder()
        int i = 0

        def patients = createPatients n
        patients.each { patient ->
            columns.each { columnVariable ->
                //println "$patient $columnVariable, ${values[i]}"
                if (values[i] != null) {
                    builder.put(patient, columnVariable, values[i] as String)
                }
                i++
            }
        }

        createPatientRows(patients, createPatientRowLabels(n), builder.build(), relaxed)
    }

    private List<PatientRow> createPatientRows(List<Patient> patients,
                                               List<String> labels,
                                               com.google.common.collect.Table<Patient, ClinicalVariableColumn, String> data,
                                               boolean relaxed) {
        dot(patients, labels) { Patient patient, String label ->
            PatientRow row = mock(PatientRow)
            row.label.returns(label).stub()
            row.patient.returns(patient).atLeastOnce()
            data.row(patient).each { column, cell ->
                if (!relaxed) {
                    row.getAt(column).returns(cell).atLeastOnce()
                } else {
                    row.getAt(column).returns(cell).stub()
                }
            }
            row
        }
    }

    private AssayColumn createMockAssay(String patientInTrialId, String label) {
        AssayColumn assayColumn = mock(AssayColumn)
        assayColumn.patientInTrialId.returns(patientInTrialId).atLeastOnce()
        assayColumn.label.returns(label).stub()
        assayColumn
    }

    private DataRow<AssayColumn, Double> createMockRow(Map<AssayColumn, Double> values, String label) {
        DataRow row = mock(DataRow)
        row.label.returns(label).stub()

        values.eachWithIndex { entry, i ->
            row.getAt(i).returns(entry.value).atLeastOnce()
        }
        // we now call end up calling getAt(Integer)
        /*values.keySet().each { column ->
            row.getAt(column).returns(values[column])
        }*/
        row
    }
}
