package org.transmartproject.db.concept

import org.junit.Test

import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*

import grails.test.mixin.*
import grails.test.mixin.support.*


@TestMixin(GrailsUnitTestMixin)
class ConceptKeyTests {

    @Test
    void basicTest() {
        def conceptKey = new ConceptKey('\\\\tableCode\\full\\thing\\')

        assertThat conceptKey, allOf(
                hasProperty('tableCode', equalTo('tableCode')),
                hasProperty('conceptFullName', hasProperty('length',
                        equalTo(2))))
    }

    @Test
    void basicTestAlternativeConstructor() {
        def conceptKey = new ConceptKey('tableCode', '\\full\\thing\\')

        assert conceptKey == new ConceptKey('\\\\tableCode\\full\\thing\\')
    }

    @Test
    void badInput() {
        def keys = ['', '\\\\', '\\\\\\', '\\\\a\\', '\\as\\b',
                '\\\\a\\b\\\\', null]

        keys.each({
            try {
                new ConceptKey(it)
                fail "Could create ConceptKey with value " + it
            } catch (Exception iae) {
                assertThat iae, isA(IllegalArgumentException)
            }
        })
    }

}
