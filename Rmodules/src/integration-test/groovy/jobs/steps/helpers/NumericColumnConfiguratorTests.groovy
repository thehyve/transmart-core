package jobs.steps.helpers

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import jobs.UserParameters
import jobs.table.Table
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.clinical.ClinicalDataResource
import org.transmartproject.core.dataquery.highdim.projections.Projection

import static jobs.steps.helpers.ConfiguratorTestsHelper.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.closeTo
import static org.hamcrest.Matchers.contains

@Integration
@Rollback
class NumericColumnConfiguratorTests {

    @Autowired
    Table table

    @Autowired
    UserParameters params

    @Autowired
    NumericColumnConfigurator testee

    @Autowired
    ClinicalDataResource clinicalDataResourceMock

    @Delegate(interfaces = false)
    ConfiguratorTestsHelper configuratorTestsHelper =
            new ConfiguratorTestsHelper()

    @Before
    void setUp() {
        initializeAsBean configuratorTestsHelper

        testee.projection = Projection.DEFAULT_REAL_PROJECTION
        testee.keyForConceptPath = 'variable'
        testee.keyForDataType = 'divVariableType'
        testee.keyForSearchKeywordId = 'divVariablePathway'
        testee.keyForLog10 = 'applyLog10'
        testee.header = 'Y'
    }

    @Test
    void testLog10Off() {
        params.@map.putAll([
                variable           : CONCEPT_PATH_CLINICAL,
                divVariableType    : DATA_TYPE_NAME_CLINICAL,
                result_instance_id1: RESULT_INSTANCE_ID1,
                result_instance_id2: RESULT_INSTANCE_ID2,
        ])

        setupClinicalResult(1,
                createClinicalVariableColumns([CONCEPT_PATH_CLINICAL]),
                [10])

        configuratorTestsHelper.play {
            testee.addColumn()
            table.buildTable()
            assertThat table.result, contains(contains(10))
        }
    }

    @Test
    void testLog10On() {
        params.@map.putAll([
                variable           : CONCEPT_PATH_CLINICAL,
                divVariableType    : DATA_TYPE_NAME_CLINICAL,
                result_instance_id1: RESULT_INSTANCE_ID1,
                result_instance_id2: RESULT_INSTANCE_ID2,
                applyLog10         : 'true',
        ])

        setupClinicalResult(1,
                createClinicalVariableColumns([CONCEPT_PATH_CLINICAL]),
                [10])

        configuratorTestsHelper.play {
            testee.addColumn()
            table.buildTable()
            assertThat table.result, contains(contains(closeTo(1.0d, 0.001d)))
        }
    }

}
