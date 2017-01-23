package org.transmartproject.utils

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test

import static ConceptUtils.shortestUniqueTails
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo

@TestMixin(GrailsUnitTestMixin)
class ConceptUtilsTests {

    @Test
    void testShortestUniqueTailsForEmpty() {
        List<String> actualResult = ConceptUtils.shortestUniqueTails([])

        assertThat actualResult, equalTo([])
    }

    @Test
    void testShortestUniqueTailsNormalization() {
        List<String> actualResult = shortestUniqueTails([
                'C',
        ])

        assertThat actualResult, equalTo([
                '\\C\\',
        ])
    }

    @Test
    void testShortestUniqueTailsForSingleConcept() {
        List<String> actualResult = shortestUniqueTails([
                '\\A\\B\\C\\',
        ])

        assertThat actualResult, equalTo([
                '\\C\\',
        ])
    }

    @Test
    void testShortestUniqueTailsForDuplicates() {
        List<String> actualResult = shortestUniqueTails([
                '\\A\\B\\C\\',
                '\\A\\B\\C\\',
        ])

        assertThat actualResult, equalTo([
                '\\A\\B\\C\\',
                '\\A\\B\\C\\',
        ])
    }

    @Test
    void testShortestUniqueTailsSameLevel() {
        List<String> actualResult = shortestUniqueTails([
                '\\1\\2\\3\\4\\',
                '\\1\\2\\3\\A\\',
                '\\1\\2\\B\\A\\',
                '\\1\\C\\B\\A\\',
                '\\D\\C\\B\\A\\',
        ])

        assertThat actualResult, equalTo([
                '\\4\\',
                '\\3\\A\\',
                '\\2\\B\\A\\',
                '\\1\\C\\B\\A\\',
                '\\D\\C\\B\\A\\',
        ])
    }

    @Test
    void testShortestUniqueTailsDiffLevels() {
        List<String> actualResult = shortestUniqueTails([
                '\\A\\',
                '\\B\\A\\',
                '\\C\\B\\A\\',
                '\\D\\C\\B\\A\\',
        ])

        assertThat actualResult, equalTo([
                '\\A\\',
                '\\B\\A\\',
                '\\C\\B\\A\\',
                '\\D\\C\\B\\A\\',
        ])
    }

}
