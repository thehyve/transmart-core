package jobs.steps.helpers

import com.google.common.collect.Lists
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import jobs.UserParameters
import jobs.table.Table
import jobs.table.columns.PrimaryKeyColumn
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.clinical.ClinicalDataResource
import org.transmartproject.core.dataquery.highdim.projections.Projection

import static jobs.steps.helpers.ConfiguratorTestsHelper.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@Integration
@Rollback
class ContextNumericVariableColumnConfiguratorTests {

    public static final String CONCEPT_PATH_HIGH_DIMENSION_EXTRA = '\\bogus\\highdim\\variable\\extra\\'
    public static final String COLUMN_HEADER = 'SAMPLE_HEADER'

    @Autowired
    ContextNumericVariableColumnConfigurator testee

    @Autowired
    Table table

    @Autowired
    UserParameters params

    @Autowired
    ClinicalDataResource clinicalDataResourceMock

    @Delegate(interfaces = false)
    ConfiguratorTestsHelper configuratorTestsHelper =
            new ConfiguratorTestsHelper()

    @Before
    void init() {
        initializeAsBean configuratorTestsHelper

        testee.header                = COLUMN_HEADER
        testee.projection            = Projection.DEFAULT_REAL_PROJECTION
        testee.keyForConceptPaths    = 'conceptPaths'
        testee.keyForDataType        = 'dataType'
        testee.keyForSearchKeywordId = 'searchKeywordId'
        testee.multiConcepts         = true
    }

    @Test
    void testMultiHighDimensional() {
        /* multiConcepts is true */
        params.@map.putAll([
                conceptPaths       : [CONCEPT_PATH_HIGH_DIMENSION,
                                      CONCEPT_PATH_HIGH_DIMENSION_EXTRA].join('|'),
                dataType           : DATA_TYPE_NAME_HIGH_DIMENSION,
                searchKeywordId    : SEARCH_KEYWORD_ID,

                result_instance_id1: RESULT_INSTANCE_ID1,
                result_instance_id2: RESULT_INSTANCE_ID2,
        ])

        def data = [
                result1: [row1: [ 0.1, 0.2, 0.3 ],
                          row2: [ 0.4, 0.5, 0.6 ]],
                result2: [row1: [      0.7, 0.8 ]]
        ]

        def assays1 = createSampleAssays(3)
        //patients ids will be the same as for assays1:
        def assays2 = createSampleAssays(3)
        TabularResult result1 = createHighDimTabularResult(
                assays: assays1,
                data:   data.result1)
        TabularResult result2 = createHighDimTabularResult(
                assays: assays2[1..2],
                data:   data.result2)

        createDataTypeResourceMock(
                (CONCEPT_PATH_HIGH_DIMENSION):       result1,
                (CONCEPT_PATH_HIGH_DIMENSION_EXTRA): result2)

        play {
            table.addColumn(new PrimaryKeyColumn(header: 'PK'), [] as Set)
            testee.addColumn()

            table.buildTable()

            def res = Lists.newArrayList table.result
            assertThat res, containsInAnyOrder(
                    contains(
                            is('patient_2_subject_id'),
                            allOf(
                                    hasEntry("$CONCEPT_PATH_HIGH_DIMENSION|row1" as String, 0.2),
                                    hasEntry("$CONCEPT_PATH_HIGH_DIMENSION|row2" as String, 0.5),
                                    hasEntry("$CONCEPT_PATH_HIGH_DIMENSION_EXTRA|row1" as String, 0.7),
                            )
                    ),
                    contains(
                            is('patient_3_subject_id'),
                            allOf(
                                    hasEntry("$CONCEPT_PATH_HIGH_DIMENSION|row1" as String, 0.3),
                                    hasEntry("$CONCEPT_PATH_HIGH_DIMENSION|row2" as String, 0.6),
                                    hasEntry("$CONCEPT_PATH_HIGH_DIMENSION_EXTRA|row1" as String, 0.8),
                            )
                    ),
            )
        }
    }

    @Test
    void testMultiClinical() {
        params.@map.putAll([
                conceptPaths       : BUNDLE_OF_CLINICAL_CONCEPT_PATH.join('|'),
                dataType           : DATA_TYPE_NAME_CLINICAL,

                result_instance_id1: RESULT_INSTANCE_ID1,
                result_instance_id2: RESULT_INSTANCE_ID2,
        ])


        def data = [1.0, 2.0, 3.0]
        setupClinicalResult(1,
                createClinicalVariableColumns(BUNDLE_OF_CLINICAL_CONCEPT_PATH),
                data)

        play {
            testee.addColumn()
            table.buildTable()

            // no maps involved here
            assertThat Lists.newArrayList(table.result), contains(
                    contains(allOf(
                            dot(['\\var 1\\', '\\var 2\\', '\\var 3\\'], data) { a, b -> hasEntry(a, b) })))
        }
    }

    @Test
    void testSingleHighDimensional() {
        /* multiConcepts is true */
        params.@map.putAll([
                conceptPaths       : CONCEPT_PATH_HIGH_DIMENSION,
                dataType           : DATA_TYPE_NAME_HIGH_DIMENSION,
                searchKeywordId    : SEARCH_KEYWORD_ID,

                result_instance_id1: RESULT_INSTANCE_ID1,
                result_instance_id2: RESULT_INSTANCE_ID2,
        ])

        def data = [row1: [0.7], row2: [0.8]]

        def assays = createSampleAssays(1)
        TabularResult result = createHighDimTabularResult(assays: assays,
                data: data)

        createDataTypeResourceMock(result)

        play {
            testee.addColumn()
            table.buildTable()

            def res = Lists.newArrayList(table.result)
            assertThat res, contains(
                    contains([
                            ("$CONCEPT_PATH_HIGH_DIMENSION|row1".toString()): 0.7,
                            ("$CONCEPT_PATH_HIGH_DIMENSION|row2".toString()): 0.8]))
        }
    }

    @Test
    void testSingleClinical() {
        params.@map.putAll([
                conceptPaths       : CONCEPT_PATH_CLINICAL,
                dataType           : DATA_TYPE_NAME_CLINICAL,

                result_instance_id1: RESULT_INSTANCE_ID1,
                result_instance_id2: RESULT_INSTANCE_ID2,
        ])

        setupClinicalResult(1,
                createClinicalVariableColumns([CONCEPT_PATH_CLINICAL]),
                [34.0])

        play {
            testee.addColumn()
            table.buildTable()

            // no maps involved here
            assertThat Lists.newArrayList(table.result), contains(
                    contains([('\\variable\\'): 34.0]))
        }
    }
}
