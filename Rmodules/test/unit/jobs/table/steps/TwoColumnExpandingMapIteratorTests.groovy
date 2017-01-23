package jobs.table.steps

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import jobs.steps.TwoColumnExpandingMapIterator
import org.junit.Test

import static jobs.table.steps.ExpandingMapIteratorTests.toList
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.contains
import static org.hamcrest.Matchers.is

@TestMixin(GrailsUnitTestMixin)
class TwoColumnExpandingMapIteratorTests {

    private TwoColumnExpandingMapIterator testee

    @Test
    void basicTest() {
        def data = [
                [1, ['foo|bar': 3, 'foo2|bar2': 4], 2]
        ]
        testee = new TwoColumnExpandingMapIterator(data.iterator(), [1])

        assertThat toList(testee), contains(
                contains([1, 3, 'foo',  'bar',  2].collect { is it as String }),
                contains([1, 4, 'foo2', 'bar2', 2].collect { is it as String }),
        )
    }

    @Test
    void testNoDataRow() {
        // the case for clinical data
        def data = [
                [1, ['foo': 3, 'bar': 4], 2]
        ]
        testee = new TwoColumnExpandingMapIterator(data.iterator(), [1])
        testee.defaultRowLabel = 'xpto'

        assertThat toList(testee), contains(
                contains([1, 3, 'foo', 'xpto', 2].collect { is it as String }),
                contains([1, 4, 'bar', 'xpto', 2].collect { is it as String }),
        )
    }

    @Test
    void testTwoTransformedColumns() {
        def data = [
                [1, ['foo|bar': 3, 'foo2|bar2': 4], ['foo3|bar3': 5, 'foo4|bar4': 6]]
        ]
        testee = new TwoColumnExpandingMapIterator(data.iterator(), [1, 2])

        assertThat toList(testee), contains(
                contains([1, 3, 'foo',  'bar',  5, 'foo3', 'bar3'].collect { is it as String }),
                contains([1, 3, 'foo',  'bar',  6, 'foo4', 'bar4'].collect { is it as String }),
                contains([1, 4, 'foo2', 'bar2', 5, 'foo3', 'bar3'].collect { is it as String }),
                contains([1, 4, 'foo2', 'bar2', 6, 'foo4', 'bar4'].collect { is it as String }),
        )
    }

}
