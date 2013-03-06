package org.transmartproject.db.concept

import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class ConceptFullNameTests {

    @Test
    void basicTest() {
        def conceptFullName = new ConceptFullName('\\abc\\d\\e')

        assertThat conceptFullName, hasProperty('length', equalTo(3))
        assert conceptFullName[0] == 'abc'
        assert conceptFullName[1] == 'd'
        assert conceptFullName[2] == 'e'
        assert conceptFullName[3] == null
        assert conceptFullName[-2] == 'd'
    }

    @Test
    void trailingSlashIsOptional() {
        def c1 = new ConceptFullName('\\a'),
            c2 = new ConceptFullName('\\a\\')

        assertThat c1, is(equalTo(c2))
    }

    @Test
    void testBadPaths() {
        def paths = ['', '\\', '\\\\', '\\a\\\\', '\\a\\\\b']

        paths.each({
            try {
                def c = new ConceptFullName(it)
                fail "Could create ConceptFullName with path " + it + ", " +
                        "result: " + c
            } catch (Exception iae) {
                assertThat iae, isA(IllegalArgumentException)
            }
        })
    }
}
