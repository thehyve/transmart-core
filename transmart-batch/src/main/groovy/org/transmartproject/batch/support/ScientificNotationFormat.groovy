package org.transmartproject.batch.support

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParsePosition

/**
 * Supports parsing of the scientific format numbers.
 * Variations like 1.002e+9 and 1.002*10^9 are allowed.
 */
class ScientificNotationFormat extends NumberFormat {

    @Delegate
    DecimalFormat decimalFormat = new DecimalFormat('0.###E0', new DecimalFormatSymbols(Locale.ENGLISH))

    /**
     * Parses the string by first normalizing the string  {@link this.normalizeString}.
     * Unlike superclass (overridden) version this method implementation throws an exception when the string
     * from the beginning to the end could not be parsed as a number.
     */
    @Override
    Number parse(String text, ParsePosition pos) {
        String normalizedString = normalizeString(text)

        Number result = decimalFormat.parse(normalizedString, pos)

        if (pos.index < normalizedString.length()) {
            throw new IllegalArgumentException("'${normalizedString}' cannot be parsed as a number" +
                    " owing to an unrecognized character at position ${pos.index + 1}")
        }

        result
    }

    private static String normalizeString(String text) {
        text
                .replace('e', 'E')
                .replace('E+', 'E')
                .replace('*10^', 'E')
    }
}
