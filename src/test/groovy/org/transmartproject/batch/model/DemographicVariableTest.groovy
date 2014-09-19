package org.transmartproject.batch.model

import org.junit.Assert
import org.junit.Test

/**
 *
 */
class DemographicVariableTest {

    @Test
    void testAgeMatch() {
        assertMatches(DemographicVariable.AGE, 'age', 'foo(AgE)', 'age (year)')
    }

    @Test
    void testGendercMatch() {
        assertMatches(DemographicVariable.GENDER, 'SeX', 'genDER', 'blaaa (sex)')
    }

    @Test
    void testRaceMatch() {
        assertMatches(DemographicVariable.RACE, 'rACe', 'foobared (race)')
    }

    @Test
    void testNoMatches() {
        assertNoMatch('age fooo')
        assertNoMatch('SEX fooo')
        assertNoMatch('RACE fooo')
    }

    private void assertMatches(DemographicVariable expected, String ... names) {
        names.each {
            DemographicVariable actual = DemographicVariable.getMatching(it)
            Assert.assertEquals("matching failed for $it", expected, actual)
        }
    }

    private void assertNoMatch(String name) {
        Assert.assertNull("$name should not have a match", DemographicVariable.getMatching(name))
    }
}
