package org.transmartproject.batch.support

import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is

/**
 * Test methods in {@link StringUtils}.
 */
class StringUtilsTest {

    @Test
    void testEscapeForLikeDefaultBackslash() {
        def input = '\\aa_%\\ds'
        def expected = '\\\\aa\\_\\%\\\\ds'

        assertThat StringUtils.escapeForLike(input), is(expected)
    }

    @Test
    void testEscapeForDifferentCharacter() {
        def input = '\\aa*_%\\ds'
        def expected = '\\aa***_*%\\ds'

        assertThat StringUtils.escapeForLike(input, '*'), is(expected)
    }

    @Test
    void testLookAlikeDespitePlural() {
        assertThat StringUtils.lookAlike('samples types', 'sample type'), is(true)
    }

    @Test
    void testLookAlikeDespiteCase() {
        assertThat StringUtils.lookAlike('TimePoints', 'TIMEPOINTS'), is(true)
    }

    @Test
    void testLookAlikeDespiteDividers() {
        assertThat StringUtils.lookAlike('tissue type', 'tissue_type'), is(true)
    }

    @Test
    void testLookAlikeDespiteAbsentDivider() {
        assertThat StringUtils.lookAlike('tissuetype', 'tissue_type'), is(true)
    }

}
