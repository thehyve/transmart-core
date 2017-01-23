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

    /**
     * Compares 2 strings ignoring case of the letters, splitters and plural form (naive).
     */
    static boolean lookAlike(String s1, String s2) {
        def normalize = { String s ->
            //remove s is naive way to make plural unimportant difference
            s.toLowerCase().replaceAll(/(?:s\b|[-_'\s]+)/, '')
        }
        normalize(s1) == normalize(s2)
    }

    static String removeLast(String s, String removeStr) {
        int ind = s.lastIndexOf(removeStr)
        if (ind >= 0) {
            s[0..<ind]
        } else {
            s
        }
    }

    /**
     * e.g. `getAllPossiblePairsSplits('a.b.c', '.') => [['a', 'b.c'], ['a.b', 'c']]`
     * @param string - source string to split to the pairs
     * @param delimiter - delimiter to use for split operation
     * @return all possible pairs splits from left to right
     */
    static List<List<String>> getAllPossiblePairsSplits(final String string, final String delimiter) {
        List<List<String>> result = []
        int indexOfDelimiter = -1
        while((indexOfDelimiter = string.indexOf(delimiter, indexOfDelimiter + 1)) > -1) {
            result << [
                string[0..<indexOfDelimiter],
                string[indexOfDelimiter + delimiter.length()..<string.length()]
            ]
        }
        result
    }
}
