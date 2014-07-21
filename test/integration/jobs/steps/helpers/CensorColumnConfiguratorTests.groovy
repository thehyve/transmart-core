package jobs.steps.helpers

import com.google.common.collect.Lists
import grails.test.mixin.TestMixin
import jobs.UserParameters
import jobs.table.Column
import jobs.table.Table
import jobs.table.columns.CensorColumn
import jobs.table.columns.PrimaryKeyColumn
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.clinical.ClinicalDataResource
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn

import static jobs.steps.helpers.ConfiguratorTestsHelper.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.contains
import static org.hamcrest.Matchers.containsInAnyOrder

@TestMixin(JobsIntegrationTestMixin)
class CensorColumnConfiguratorTests {

    @Autowired
    Table table

    @Autowired
    UserParameters params

    @Autowired
    CensorColumnConfigurator testee

    @Autowired
    ClinicalDataResource clinicalDataResourceMock

    @Delegate(interfaces = false)
    ConfiguratorTestsHelper configuratorTestsHelper =
            new ConfiguratorTestsHelper()

    @Before
    void setUp() {
        initializeAsBean configuratorTestsHelper

        testee.header = 'CENSOR'
        testee.keyForConceptPaths = 'censoringVariable'
    }

    @Test
    void testCensoring() {
        params.@map.putAll([
                censoringVariable: BUNDLE_OF_CLINICAL_CONCEPT_PATH.join('|'),
                result_instance_id1: RESULT_INSTANCE_ID1,
                result_instance_id2: RESULT_INSTANCE_ID2,
        ])

        List<ClinicalVariableColumn> columns =
                createClinicalVariableColumns(BUNDLE_OF_CLINICAL_CONCEPT_PATH)
        setupClinicalResult(2, columns, ['var 1', null, 'var 3',
                null, null, null])

        configuratorTestsHelper.play {
            table.addColumn(new PrimaryKeyColumn(header: 'PK'), [] as Set)
            testee.addColumn()
            table.buildTable()

            def res = Lists.newArrayList table.result
            assertThat res,
                    containsInAnyOrder(
                            contains(
                                    'subject id #1',
                                    CensorColumn.CENSORING_TRUE
                            ),
                            contains(
                                    'subject id #2',
                                    CensorColumn.CENSORING_FALSE
                            )
                    )

        }
    }

    @Test
    void testCensoringUnspecified() {
        params.@map.putAll([
                censoringVariable: '',
                result_instance_id1: RESULT_INSTANCE_ID1,
                result_instance_id2: RESULT_INSTANCE_ID2,
        ])

        configuratorTestsHelper.play {
            table.addColumn(new PrimaryKeyColumn(header: 'PK'), [] as Set)

            Column stubColumn = new StubColumn(header: 'STUB',
                    data: ['subject id #1': 23, 'subject id #2': 42])
            table.addColumn stubColumn, Collections.emptySet()

            testee.addColumn()
            table.buildTable()

            def res = Lists.newArrayList table.result
            assertThat res,
                    containsInAnyOrder(
                            contains(
                                    'subject id #1',
                                    23,
                                    CensorColumn.CENSORING_TRUE
                            ),
                            contains(
                                    'subject id #2',
                                    42,
                                    CensorColumn.CENSORING_TRUE
                            )
                    )

        }
    }

}
