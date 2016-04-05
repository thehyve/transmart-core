package org.transmartproject.batch.support

import org.hamcrest.MatcherAssert
import org.junit.Test

import static org.hamcrest.Matchers.closeTo
import static org.hamcrest.Matchers.equalTo

/**
 * Tests {@link ScientificNotationFormat}.
 */
class ScientificNotationFormatTests {

    private static final double ERROR = 0.005d

    ScientificNotationFormat testee = new ScientificNotationFormat()

    @Test
    void testLowerCaseExponentSymbol() {
        Number number = testee.parse('1e2')

        MatcherAssert.assertThat number, equalTo(100L)
    }

    @Test
    void testAlternativeExponentFormat() {
        Number number = testee.parse('1*10^2')

        MatcherAssert.assertThat number, equalTo(100L)
    }

    @Test
    void testPlusInPowerIsAllowed() {
        Number number = testee.parse('1e+2')

        MatcherAssert.assertThat number, equalTo(100L)
    }

    @Test
    void testScientificVariationSupport() {
        Number number = testee.parse('1.002e+9')

        MatcherAssert.assertThat number, equalTo(1002000000L)
    }


    @Test
    void testAllDigitalPositionsParsed() {
        Number number = testee.parse('1.234567890E9')

        MatcherAssert.assertThat number, equalTo(1234567890L)
    }

    @Test
    void testNonScientificNotationSupportPreserved() {
        Number number = testee.parse('12345.67890')

        MatcherAssert.assertThat number,  closeTo(12345.67890d, ERROR)
    }
}
