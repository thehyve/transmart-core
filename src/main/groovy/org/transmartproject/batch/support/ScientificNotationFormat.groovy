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

    private static final Locale LOCALE = Locale.ENGLISH.getDefault(Locale.Category.FORMAT)

    @Delegate
    DecimalFormat decimalFormat = new DecimalFormat('0.###E0', new DecimalFormatSymbols(LOCALE))

    Number parse(String text, ParsePosition pos) {
        String normalizedString = normalizeString(text)

        Number result = decimalFormat.parse(normalizedString, pos)

        if (pos.index < normalizedString.length()) {
            throw new IllegalArgumentException("Number (${text}) contains unrecognized characters.")
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
