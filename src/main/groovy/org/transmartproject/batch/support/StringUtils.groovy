package org.transmartproject.batch.support

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Misc string utils
 */
class StringUtils {

    static String escapeForLike(String s, char escapeChar = '\\') {
        s.replaceAll(/[${Pattern.quote(escapeChar as String)}_%]/,
                "${Matcher.quoteReplacement(escapeChar as String)}\$0")
    }
}
