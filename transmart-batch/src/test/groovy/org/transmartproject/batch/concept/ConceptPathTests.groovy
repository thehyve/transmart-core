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

    @Test
    void testEscapePlus() {
        ConceptFragment decodedConcept = ConceptFragment.decode('A+B\\+_C')

        assertThat decodedConcept, allOf(
                hasProperty('path', equalTo('A\\B+ C\\')),
                hasProperty('parts', contains('A', 'B+ C'))
        )

        decodedConcept = ConceptFragment.decode('A+B+_C\\+')

        assertThat decodedConcept, allOf(
                hasProperty('path', equalTo('A\\B\\ C+\\')),
                hasProperty('parts', contains('A', 'B', ' C+'))
        )

        decodedConcept = ConceptFragment.decode('\\+A+B+_C\\+')

        assertThat decodedConcept, allOf(
                hasProperty('path', equalTo('+A\\B\\ C+\\')),
                hasProperty('parts', contains('+A', 'B', ' C+'))
        )
    }

    @Test
    void testEscapeUnderscore() {
        ConceptFragment decodedConcept = ConceptFragment.decode('A+B\\_C')

        assertThat decodedConcept, allOf(
                hasProperty('path', equalTo('A\\B_C\\')),
                hasProperty('parts', contains('A', 'B_C'))
        )

        decodedConcept = ConceptFragment.decode('A+B+_C\\_')

        assertThat decodedConcept, allOf(
                hasProperty('path', equalTo('A\\B\\ C_\\')),
                hasProperty('parts', contains('A', 'B', ' C_'))
        )

        decodedConcept = ConceptFragment.decode('\\_A+B+_C')

        assertThat decodedConcept, allOf(
                hasProperty('path', equalTo('_A\\B\\ C\\')),
                hasProperty('parts', contains('_A', 'B', ' C'))
        )
    }
}
