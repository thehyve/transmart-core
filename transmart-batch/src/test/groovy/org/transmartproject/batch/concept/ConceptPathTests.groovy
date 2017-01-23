package org.transmartproject.batch.concept

import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Tests for {@link ConceptPath} and {@link ConceptFragment}
 */
class ConceptPathTests {

    @Test
    void testDecodeConcept() {
        ConceptFragment decodedConcept = ConceptFragment.decode('A+B_C')

        assertThat decodedConcept, allOf(
                hasProperty('path', equalTo('A\\B C\\')),
                hasProperty('parts', contains('A', 'B C')),
        )
    }
}
