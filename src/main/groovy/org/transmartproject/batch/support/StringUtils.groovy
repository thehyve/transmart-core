package org.transmartproject.batch.support

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Misc string utils
 */
class StringUtils {

    static String escapeForSQLString(String s) {
        s.replaceAll(/'/, "''")
    }

    static String escapeForLike(String s, String escapeChar = '\\') {
        s.replaceAll(/(?:${Pattern.quote(escapeChar)}|[_%])/,
                "${Matcher.quoteReplacement(escapeChar)}\$0")
    }
}
