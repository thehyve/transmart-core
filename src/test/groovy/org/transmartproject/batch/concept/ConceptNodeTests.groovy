package org.transmartproject.batch.concept

import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Unit test for @see ConceptNode
 */
class ConceptNodeTests {

    @Test
    void testConceptNodeIsNew() {
        def testee = new ConceptNode(code: 'foo')

        assertThat testee.new, is(true)
    }

    @Test
    void testConceptNodeIsOld() {
        def testee = new ConceptNode(i2b2RecordId: 1, code: 'bar')

        assertThat testee.new, is(false)
    }

}
