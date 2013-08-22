package com.recomdata.transmart.util;

import java.util.HashMap;
import java.util.Map;

public class RUtil {

    private static Map<Integer, String> ESCAPE_SEQUENCES = new HashMap<Integer, String>();

    static {
        ESCAPE_SEQUENCES.put(0x07, "\\a"); // Bell
        ESCAPE_SEQUENCES.put(0x08, "\\b"); // Backspace
        ESCAPE_SEQUENCES.put(0x0C, "\\f"); // Form feed
        ESCAPE_SEQUENCES.put(0x0A, "\\n"); // Line feed
        ESCAPE_SEQUENCES.put(0x0D, "\\r"); // Carriage return
        ESCAPE_SEQUENCES.put(0x09, "\\t"); // Horizontal tab
        ESCAPE_SEQUENCES.put(0x0B, "\\v"); // Vertical tab
        ESCAPE_SEQUENCES.put(0x5C, "\\\\"); // Backslash
        ESCAPE_SEQUENCES.put(0x22, "\\\""); // Quotation mark
        ESCAPE_SEQUENCES.put(0x27, "\\'"); // Apostrophe
    }

    /**
     * Escapes a string so that it's safe to include inside a single- or
     * double-quoted R string. The result will only contain ASCII
     * characters so the string can be serialized to any (reasonable) byte
     * stream.
     * @param s
     * @return
     */
    public static String escapeRStringContent(String s) {
        /* ok to overflow: will just give a NegativeArraySizeException */
        StringBuffer sb = new StringBuffer(s.length() * 2);

        /* See StringValue() in R's lexer file (gram.y)
         * Mixing \xXX and \\uXXXX escape sequences is forbidden. If a string
         * contains a \\u (or \U) sequence, it will be stored internally in
         * UTF-8. Functions that use such string can generally choose
         * having UTF-8 strings converted to the current locale or work
         * with the UTF-8 data directly. See:
         * http://cran.r-project.org/doc/manuals/R-ints.html#Encodings-for-CHARSXPs */
        final int length = s.length();
        for (int offset = 0; offset < length; ) {
            final int cp = s.codePointAt(offset);

            if (cp >= 0x80) { /* high characters */
                if (cp <= 0xFFFF) {
                    sb.append(String.format("\\u%04X", cp));
                } else {
                    sb.append(String.format("\\U%08X", cp));
                }
            } else if (ESCAPE_SEQUENCES.containsKey(cp)) {
                sb.append(ESCAPE_SEQUENCES.get(cp));
            } else if (cp < 0x20 || cp == 0x7F) {
                /* other control characters. This will force the string to
                 * be in UTF-8, which could otherwise not be necessary, but
                 * doing it this way keeps the code a little simpler */
                sb.append(String.format("\\u%04X", cp));
            } else {
                sb.appendCodePoint(cp);
            }

            offset += Character.charCount(cp);
        }

        return sb.toString();
    }

}
