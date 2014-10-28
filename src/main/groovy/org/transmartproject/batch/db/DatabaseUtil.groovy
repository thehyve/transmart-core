package org.transmartproject.batch.db

/**
 * Constants for schemas and sequences in database
 */
final class DatabaseUtil {

    private DatabaseUtil() {}

    static void checkUpdateCounts(int[] counts, String operation) {
        if (!counts.every { it == 1 }) {
            throw new RuntimeException("Updated rows mismatch while $operation")
        }
    }

}
