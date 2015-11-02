package org.transmartproject.batch.db

import com.google.common.collect.Lists
import groovy.util.logging.Slf4j
import org.springframework.batch.core.UnexpectedJobExecutionException
import org.springframework.dao.IncorrectResultSizeDataAccessException

/**
 * Constants for schemas and sequences in database
 */
@Slf4j
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

    static void checkUpdateCountsPermissive(int[] counts,
                                            String operation,
                                            List objects,
                                            boolean allowMultiple = false) {
        checkUpdateCountsPermissive(
                Lists.newArrayList(counts), operation, objects, allowMultiple)
    }

    static void checkUpdateCountsPermissive(List<Integer> counts,
                                            String operation,
                                            List objects,
                                            boolean allowMultiple = false) {
        List foundZeros = []
        counts.eachWithIndex { Integer c, int index ->
            if (c > 1) {
                def msg = "$operation resulted in more than one row being " +
                        'affected for at least one object. Check for duplicates.'
                try {
                    if (allowMultiple) {
                        log.warn(msg)
                    } else {
                        throw new UnexpectedJobExecutionException(msg)
                    }
                } finally {
                    log.warn "Culprit object was: ${objects[index]}"
                }
            } else if (c == 0) {
                foundZeros << objects[index]
            } // else it == 1; normal
        }

        if (foundZeros) {
            log.warn("$operation resulted " +
                    "in no matches for ${foundZeros.size()} object(s) in this chunk")
            log.warn("Culprits were: $foundZeros")
        }
    }

    static String asLikeLiteral(String s) {
        s.replaceAll(/[\\%_]/, '\\\\$0')
    }

}
