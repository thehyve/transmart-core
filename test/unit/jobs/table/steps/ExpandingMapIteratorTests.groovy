package jobs.table.steps

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import jobs.steps.ExpandingMapIterator
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@TestMixin(GrailsUnitTestMixin)
class ExpandingMapIteratorTests {

    private ExpandingMapIterator testee

    @Test
    void testOneLineTwoMaps() {
        testee = new ExpandingMapIterator(
                [['A1', [a: 1, b: 2], 'B1', [c: 3, d: 4]]].iterator(),
                [1, 3])

        def res = toList testee
        assertThat res, containsInAnyOrder(
                [ 'A1', '1', 'a', 'B1', '3', 'c' ],
                [ 'A1', '1', 'a', 'B1', '4', 'd' ],
                [ 'A1', '2', 'b', 'B1', '3', 'c' ],
                [ 'A1', '2', 'b', 'B1', '4', 'd' ])
    }

    @Test
    void testOneMap() {
        testee = new ExpandingMapIterator(
                [[[a: 1, b: 2], 'A1']].iterator(),
                [0])

        assertThat toList(testee), containsInAnyOrder(
                [ '1', 'a', 'A1'],
                [ '2', 'b', 'A1'])
    }

    @Test
    void testEmptyMap() {
        testee = new ExpandingMapIterator(
                [['A1', [a: 1, b: 2], [:]]].iterator(),
                [1, 2])

        assertThat toList(testee), is(empty())
    }

    @Test
    void testMultipleRows() {
        testee = new ExpandingMapIterator(
                [['A1', [a: 1], 'B1', [c: 3, d: 4]],
                 ['A2', [a: 1], 'B2', [c: 3, d: 4]],].iterator(),
                [1, 3])

        assertThat toList(testee), containsInAnyOrder(
                [ 'A1', '1', 'a', 'B1', '3', 'c' ],
                [ 'A1', '1', 'a', 'B1', '4', 'd' ],
                [ 'A2', '1', 'a', 'B2', '3', 'c' ],
                [ 'A2', '1', 'a', 'B2', '4', 'd' ])
    }

    @Test
    void testThreeColumns() {
        testee = new ExpandingMapIterator(
                [['A1', [a: 1], 'B1', [c: 3, d: 4], [e: 5, f: 6]]].iterator(),
                [1, 3, 4])

        assertThat toList(testee), containsInAnyOrder(
                [ 'A1', '1', 'a', 'B1', '3', 'c', '5', 'e' ],
                [ 'A1', '1', 'a', 'B1', '3', 'c', '6', 'f' ],
                [ 'A1', '1', 'a', 'B1', '4', 'd', '5', 'e' ],
                [ 'A1', '1', 'a', 'B1', '4', 'd', '6', 'f' ])
    }

    @Test
    void testNoMaps() {
        shouldFail IllegalArgumentException, {
            testee = new ExpandingMapIterator(
                    [['a', 'b']].iterator(),
                    [])
            toList testee
        }
    }

    public static def toList(Iterator<String[]> iter) {
        iter.collect { String[] it ->
            //need to copy the array because the same instance is
            //returned on every iteration
            Arrays.asList(Arrays.copyOf(it, it.length))
        }
    }
}
