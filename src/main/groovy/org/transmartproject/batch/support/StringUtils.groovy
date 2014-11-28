package org.transmartproject.batch.support

/**
 * Misc string utils
 */
class StringUtils {

    static String escapeForLike(String s, char escapeChar = '\\') {
        s.replaceAll(/[\\_%]/, "$escapeChar\\\$0")
    }
}
