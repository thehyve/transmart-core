package org.transmartproject.batch.db

import com.google.common.collect.Lists
import org.springframework.dao.IncorrectResultSizeDataAccessException

/**
 * Constants for schemas and sequences in database
 */
final class DatabaseUtil {

    private DatabaseUtil() {}

    static void checkUpdateCounts(int[] counts, String operation) {
        checkUpdateCounts Lists.newArrayList(counts), operation
    }

    static void checkUpdateCounts(List<Integer> counts, String operation) {
        if (!counts.every { it == 1 }) {
            throw new IncorrectResultSizeDataAccessException(
                    "Updated rows mismatch while $operation")
        }
    }

    static String asLikeLiteral(String s) {
        s.replaceAll(/[\\%_]/, '\\\\$0')
    }

}
