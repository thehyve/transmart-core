package jobs.table.steps

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import jobs.steps.TwoColumnExpandingMapIterator
import org.junit.Test

import static jobs.table.steps.ExpandingMapIteratorTests.toList
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

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


}
