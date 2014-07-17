package jobs.steps.helpers

import com.google.common.collect.Lists
import grails.test.mixin.TestMixin
import jobs.UserParameters
import jobs.table.Table
import jobs.table.columns.CensorColumn
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

        testee.header             = 'CENSOR'
        testee.keyForConceptPaths = 'censoringVariable'
    }

    @Test
    void testCensoring() {
        params.@map.putAll([
                censoringVariable  : BUNDLE_OF_CLINICAL_CONCEPT_PATH[0],
                result_instance_id1: RESULT_INSTANCE_ID1,
                result_instance_id2: RESULT_INSTANCE_ID2,
        ])

        List<ClinicalVariableColumn> columns =
                createClinicalVariableColumns([BUNDLE_OF_CLINICAL_CONCEPT_PATH[0]])
        setupClinicalResult(1, columns, ['var 1'])

        configuratorTestsHelper.play {
            testee.addColumn()
            table.buildTable()

            assertThat table.result, contains(contains(CensorColumn.CENSORING_TRUE))

        }
    }

    @Test
    void testCensoringMultiple() {
        params.@map.putAll([
                censoringVariable  : BUNDLE_OF_CLINICAL_CONCEPT_PATH.join('|'),
                result_instance_id1: RESULT_INSTANCE_ID1,
                result_instance_id2: RESULT_INSTANCE_ID2,
        ])

        List<ClinicalVariableColumn> columns =
                createClinicalVariableColumns(BUNDLE_OF_CLINICAL_CONCEPT_PATH)
        setupClinicalResult(columns, [['var 1', null, 'var 3'],
                                      [null, null, null]])

        configuratorTestsHelper.play {
            testee.addColumn()
            table.buildTable()

            def res = Lists.newArrayList table.result
            assertThat res,
                    containsInAnyOrder(contains(CensorColumn.CENSORING_TRUE),
                                       contains(CensorColumn.CENSORING_FALSE))

        }
    }

}
