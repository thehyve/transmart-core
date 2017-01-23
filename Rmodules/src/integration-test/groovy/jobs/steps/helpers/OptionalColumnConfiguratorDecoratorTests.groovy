package jobs.steps.helpers

import grails.test.mixin.TestMixin
import jobs.UserParameters
import jobs.table.Column
import jobs.table.Table
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.contains
import static org.hamcrest.Matchers.is

@TestMixin(JobsIntegrationTestMixin)
class OptionalColumnConfiguratorDecoratorTests {

    public static final String COLUMN_HEADER = 'SAMPLE_HEADER'
    public static final String FALLBACK_VALUE = 'FALLBACK'
    public static final String KEY_FOR_ENABLED = 'enabled'

    @Autowired
    OptionalColumnConfiguratorDecorator testee

    @Autowired
    SimpleAddColumnConfigurator simpleAddColumnConfigurator

    @Autowired
    Table table

    @Autowired
    UserParameters params

    @Before
    void before() {
        Map data = [a: 1, b: 2]
        simpleAddColumnConfigurator.column = new StubColumn(
                header: COLUMN_HEADER, data: data)

        testee.header = COLUMN_HEADER

        testee.generalCase = simpleAddColumnConfigurator
        testee.constantColumnFallback = FALLBACK_VALUE
        testee.keyForEnabled = KEY_FOR_ENABLED
    }

    @Test
    void testSetConstantColumnFallbackIsUsed() {
        Column stubColumn = new StubColumn(header: COLUMN_HEADER, data: [c: 3])
        table.addColumn stubColumn, Collections.emptySet()
        testee.addColumn()

        table.buildTable()

        def res = table.result
        assertThat res, contains(is([3, FALLBACK_VALUE]))
    }

    @Test
    void testGoesToTheGeneralCase() {
        params.@map.putAll([
                (KEY_FOR_ENABLED): 'something',
        ])

        testee.addColumn()

        table.buildTable()

        def res = table.result
        assertThat res, contains(is([1]), is([2]))
    }

}
