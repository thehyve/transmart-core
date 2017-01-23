package org.transmartproject.batch.batchartifacts

import org.transmartproject.batch.support.TokenizerColumnsReplacingHeaderHandler

/**
 * Header handler that allows a set of column names, regardless of case, and
 * normalizes them into uppercase.
 */
class UnorderedColumnsHeaderHandler implements TokenizerColumnsReplacingHeaderHandler {

    final Set<String> expectedColumnNameSet

    UnorderedColumnsHeaderHandler(Collection<String> columns) {
        expectedColumnNameSet = columns*.toUpperCase(Locale.ENGLISH) as Set
    }

    @Override
    List<String> handleLine(List<String> tokenizedHeader) {
        Set<String> normalizedArgument =
                tokenizedHeader*.toUpperCase(Locale.ENGLISH) as Set

        Set<String> extra = normalizedArgument - expectedColumnNameSet
        if (extra) {
            throw new IllegalArgumentException("Invalid column headers: $extra")
        }

        Set<String> missing = expectedColumnNameSet - normalizedArgument
        if (missing) {
            throw new IllegalArgumentException(
                    "Missing column headers: $missing")
        }

        normalizedArgument
    }
}
