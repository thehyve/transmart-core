package org.transmartproject.batch.support

import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
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

        assertThat number, equalTo(100L)
    }

    @Test
    void testAlternativeExponentFormat() {
        Number number = testee.parse('1*10^2')

        assertThat number, equalTo(100L)
    }

    @Test
    void testPlusInPowerIsAllowed() {
        Number number = testee.parse('1e+2')

        assertThat number, equalTo(100L)
    }

    @Test
    void testScientificVariationSupport() {
        Number number = testee.parse('1.002e+9')

        assertThat number, equalTo(1002000000L)
    }


    @Test
    void testAllDigitalPositionsParsed() {
        Number number = testee.parse('1.234567890E9')

        assertThat number, equalTo(1234567890L)
    }

    @Test
    void testNonScientificNotationSupportPreserved() {
        Number number = testee.parse('12345.67890')

        assertThat number, closeTo(12345.67890d, ERROR)
    }

    @Test(expected = IllegalArgumentException)
    void testExceptionOnPartlyParsableString() {
        testee.parse('123abc')
    }

    @Test
    void testEnvironmentLocaleSettingIndependence() {
        Locale defaultLocale = Locale.default

        try {
            //German locale (as many others) has comma as decimal separator.
            Locale.setDefault(Locale.GERMAN)
            //To pick up new locale
            testee = new ScientificNotationFormat()
            Number number = testee.parse('1.2')

            assertThat number, closeTo(1.2d, ERROR)
        } finally {
            Locale.setDefault(defaultLocale)
        }
    }

}
