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
}
