package org.transmartproject.batch.model

import org.junit.Test
import org.transmartproject.batch.patient.DemographicVariable

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
            assert expected == actual: "matching failed for $it"
        }
    }

    private void assertNoMatch(String name) {
        assert DemographicVariable.getMatching(name) == null: "$name should not have a match"
    }
}
