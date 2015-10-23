package org.transmartproject.batch.concept

import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Unit test for @see ConceptTree
 */
class ConceptTreeTests {

    @Test(expected = IllegalArgumentException)
    void testFailOnLoadingNewConceptsAsExisting() {
        def testee = new ConceptTree()

        def newConceptNode = [
                isNew: { true },
                getPath: { new ConceptPath('\\foo\\') }
        ] as ConceptNode
        testee.loadExisting([ newConceptNode])
    }

    @Test
    void testLoadExisting() {
        def testee = new ConceptTree()

        def path = new ConceptPath('\\foo\\')
        def existingConceptNode = [
                isNew: { false },
                getPath: { path }
        ] as ConceptNode
        testee.loadExisting([ existingConceptNode])

        assertThat testee.allConceptNodes, contains(
                hasProperty('path', equalTo(path))
        )
    }

}
