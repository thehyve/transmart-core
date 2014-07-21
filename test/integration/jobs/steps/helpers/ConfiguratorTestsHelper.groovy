package jobs.steps.helpers

import com.google.common.collect.ImmutableTable
import org.gmock.GMockController
import org.springframework.beans.factory.annotation.Autowired
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

import static jobs.steps.OpenHighDimensionalDataStep.createConceptKeyFrom
import static org.hamcrest.Matchers.*

class ConfiguratorTestsHelper {

    public static final String RESULT_INSTANCE_ID1 = '77'
    public static final String RESULT_INSTANCE_ID2 = '78'

    public static final String SEARCH_KEYWORD_ID = '88888'
    public static final String DATA_TYPE_NAME_HIGH_DIMENSION = 'mrna'
    public static final String CONCEPT_PATH_HIGH_DIMENSION = '\\bogus\\highdim\\variable\\'

    public static final String DATA_TYPE_NAME_CLINICAL = 'CLINICAL'
    public static final String CONCEPT_PATH_CLINICAL = '\\bogus\\clinical\\variable\\'
    public static final List<String> BUNDLE_OF_CLINICAL_CONCEPT_PATH = [
            '\\bogus\\clinical\\variable\\var 1',
            '\\bogus\\clinical\\variable\\var 2',
            '\\bogus\\clinical\\variable\\var 3',
    ]

    @Autowired
    QueriesResource queriesResourceMock

    @Autowired
    HighDimensionResource highDimensionResourceMock

    @Autowired
    ClinicalDataResource clinicalDataResourceMock

    @Delegate(interfaces = false)
    @Autowired
    GMockController gMockController

    ArrayList<QueryResult> mockQueryResults() {
        def queryResults = [mock(QueryResult), mock(QueryResult)]
        queriesResourceMock.getQueryResultFromId(RESULT_INSTANCE_ID1 as int).returns(queryResults[0])
        queriesResourceMock.getQueryResultFromId(RESULT_INSTANCE_ID2 as int).returns(queryResults[1])
        queryResults
    }

    void createDataTypeResourceMock(TabularResult<Double, AssayColumn> highDimResult) {
        createDataTypeResourceMock([(CONCEPT_PATH_HIGH_DIMENSION): highDimResult])
    }

    TabularResult createHighDimTabularResult(Map params) {
        List<AssayColumn> sampleAssays        = params.assays
        Map<String, List<Number>> labelToData = params.data
        String columnsDimensionLabel          = params.columnsLabel
        String rowsDimensionLabel             = params.rowsLabel

        TabularResult highDimResult = mock TabularResult
        highDimResult.indicesList.returns(sampleAssays).atLeastOnce()

        def rowsIterator = labelToData.collect { String label,
                                                 List<Number> data ->
            createRowForAssays(sampleAssays, data, label)
        }.iterator()
        highDimResult.getRows().returns(rowsIterator).stub()
        highDimResult.iterator().returns(rowsIterator).stub()

        if (columnsDimensionLabel) {
            highDimResult.columnsDimensionLabel.returns columnsDimensionLabel
        }
        if (rowsDimensionLabel) {
            highDimResult.rowsDimensionLabel.returns rowsDimensionLabel
        }

        highDimResult
    }

    void createDataTypeResourceMock(Map<String, TabularResult<Double, AssayColumn>> highDimResults) {

        HighDimensionDataTypeResource dataTypeResourceMock =
                mock(HighDimensionDataTypeResource)

        def termConstraintToResult = [:]

        highDimensionResourceMock.getSubResourceForType(DATA_TYPE_NAME_HIGH_DIMENSION).
                returns(dataTypeResourceMock)

        def projection = mock(Projection)
        dataTypeResourceMock.createProjection(
                [:], Projection.DEFAULT_REAL_PROJECTION).returns(projection)

        def disjunctionConstraint = mock(AssayConstraint)
        dataTypeResourceMock.createAssayConstraint(AssayConstraint.DISJUNCTION_CONSTRAINT,
                subconstraints: [
                        (AssayConstraint.PATIENT_SET_CONSTRAINT): [
                                [result_instance_id: RESULT_INSTANCE_ID1 as Long],
                                [result_instance_id: RESULT_INSTANCE_ID2 as Long],
                        ]]).returns(disjunctionConstraint)

        /* a bit overconstrained here, no reason we can't create
         * separate constraints/projections for each concept */

        highDimResults.each { String conceptPath, TabularResult result ->
            def mockConstraint = mock(AssayConstraint)
            termConstraintToResult[mockConstraint] = result

            dataTypeResourceMock.createAssayConstraint(AssayConstraint.ONTOLOGY_TERM_CONSTRAINT,
                    concept_key: createConceptKeyFrom(conceptPath)).returns(mockConstraint)
        }

        def searchKeywordConstraint = mock(DataConstraint)
        dataTypeResourceMock.createDataConstraint(DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT,
                keyword_ids: [SEARCH_KEYWORD_ID]).returns(searchKeywordConstraint)

        termConstraintToResult.each { AssayConstraint ontologyTermConstraint,
                                      TabularResult result ->
            dataTypeResourceMock.retrieveData(
                    containsInAnyOrder(
                            is(disjunctionConstraint),
                            is(ontologyTermConstraint)),
                    contains(searchKeywordConstraint),
                    is(projection)).returns(result)
        }
    }

    List<AssayColumn> createSampleAssays(int n) {
        (1..n).collect {
            createMockAssay("patient_${it}_subject_id",
                    "sample_code_${it}")
        }
    }

    DataRow createRowForAssays(List<AssayColumn> assays,
                               List<Double> data,
                               String label) {
        createMockRow(
                dot(assays, data, {a, b -> [ a, b ]})
                        .collectEntries(Closure.IDENTITY),
                label)
    }

    static List dot(List list1, List list2, function) {
        def res = []
        for (int i = 0; i < list1.size(); i++) {
            res << function(list1[i], list2[i])
        }
        res
    }

    List<Patient> createPatients(int n) {
        (1..n).collect {
            Patient p = mock(Patient)
            p.inTrialId.returns("subject id #$it".toString()).atLeastOnce()
            p
        }
    }

    List<String> createPatientRowLabels(int n) {
        (1..n).collect {
            "patient #$it" as String
        }
    }

    private static Object NULL_PLACEHOLDER = new Object()

    List<PatientRow> createPatientRows(int n,
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
                    builder.put(patient, columnVariable, values[i])
                } else {
                    builder.put(patient, columnVariable, NULL_PLACEHOLDER)
                }
                i++
            }
        }

        createPatientRows(patients, createPatientRowLabels(n), builder.build(), relaxed)
    }

    List<PatientRow> createPatientRows(List<Patient> patients,
                                       List<String> labels,
                                       com.google.common.collect.Table<Patient, ClinicalVariableColumn, Object> data,
                                       boolean relaxed) {
        dot(patients, labels) { Patient patient, String label ->
            PatientRow row = mock(PatientRow)
            row.label.returns(label).stub()
            row.patient.returns(patient).atLeastOnce()
            data.row(patient).each { column, cell ->
                def value = cell.is(NULL_PLACEHOLDER) ? null : cell
                if (!relaxed) {
                    row.getAt(column).returns(value).atLeastOnce()
                } else {
                    row.getAt(column).returns(value).stub()
                }
            }
            row
        }
    }

    private AssayColumn createMockAssay(String patientInTrialId, String label) {
        AssayColumn assayColumn = mock(AssayColumn)
        assayColumn.patientInTrialId.returns(patientInTrialId).stub()
        assayColumn.label.returns(label).stub()
        assayColumn
    }

    private DataRow<AssayColumn, Double> createMockRow(Map<AssayColumn, Double> values, String label) {
        DataRow row = mock(DataRow)
        row.label.returns(label).stub()

        values.eachWithIndex { entry, i ->
            row.getAt(i).returns(entry.value).stub()
        }

        values.keySet().each { column ->
            row.getAt(column).returns(values[column]).stub()
        }

        row.iterator().returns(values.values.iterator()).stub()
        row
    }

    List<ClinicalVariableColumn> createClinicalVariableColumns(List<String> conceptPaths,
                                                               boolean needsLabel = false) {
        List<ClinicalVariableColumn> columns = conceptPaths.collect {
            mock(ClinicalVariableColumn)
        }
        dot(columns, conceptPaths) { ClinicalVariableColumn col, String path ->
            if (needsLabel) {
                col.label.returns(path).atLeastOnce()
            }

            clinicalDataResourceMock.createClinicalVariable(
                    ClinicalVariable.TERMINAL_CONCEPT_VARIABLE,
                    concept_path: path).returns(col)
        }

        columns
    }

    void setupClinicalResult(int nPatients,
                             List<ClinicalVariableColumn> columns,
                             List<Object> valuesForColumns) {
        assert nPatients * columns.size() == valuesForColumns.size()

        TabularResult<ClinicalVariableColumn, PatientRow> clinicalResult =
                mock(TabularResult)
        clinicalResult.iterator().returns(createPatientRows(nPatients, columns,
                valuesForColumns, true /* relaxed */).iterator())
        clinicalResult.close().stub()

        clinicalDataResourceMock.retrieveData(
                mockQueryResults(),
                containsInAnyOrder(columns.collect { is it })).returns(clinicalResult)
    }

}
