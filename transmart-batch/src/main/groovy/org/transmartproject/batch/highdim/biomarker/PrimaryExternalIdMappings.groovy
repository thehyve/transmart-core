package org.transmartproject.batch.highdim.biomarker

import com.google.common.collect.Maps
import groovy.util.logging.Slf4j

/**
 * Encapsulates a map between case insensitive bio marker names and primary
 * external ids.
 *
 * @param T type of the primary external id
 */
@Slf4j
class PrimaryExternalIdMappings<T> {
    String biomarkerType
    String organism
    Class<T> primaryExternalIdType // String or Long

    int discardCount = 0

    Map<String, Object> mapping = Maps.newHashMap()

    def getAt(String bioMarkerName) {
        mapping[toKey(bioMarkerName)]
    }

    void putAt(String bioMarkerName, T origValue) {
        T value = origValue
        if (!bioMarkerName) {
            log.warn("Got biomarker with empty name " +
                    "(primary external id: $value); skipped")
            discardCount++
            return
        }

        if (!bioMarkerName) {
            log.warn("Got biomarker with empty primary external id (name: " +
                    "$bioMarkerName); skipped")
            discardCount++
            return
        }

        if (primaryExternalIdType == Long && value instanceof String) {
            if (!value.isLong()) {
                log.warn("Primary external id $value is not a long; skipped")
                discardCount++
                return
            }

            value = value.toLong()
        }


        String key = toKey(bioMarkerName)
        if (mapping.containsKey(key)) {
            log.warn("Biomarker with name '$bioMarkerName' and primary " +
                    "external id '$value' clashes with already existing " +
                    "entry that has primary external id '${mapping[key]}'; " +
                    'skipped')
            discardCount++
            return
        }

        /* and finally... */
        mapping[key] = value
    }

    int getCount() {
        mapping.size()
    }

    private static String toKey(String biomarkerName) {
        biomarkerName.toLowerCase()
    }
}
