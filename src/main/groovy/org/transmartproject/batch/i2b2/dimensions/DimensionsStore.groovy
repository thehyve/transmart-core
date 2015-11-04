package org.transmartproject.batch.i2b2.dimensions

import com.google.common.base.Function
import com.google.common.base.Predicate
import com.google.common.collect.*
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.AfterStep

import static org.transmartproject.batch.i2b2.variable.PatientDimensionI2b2Variable.PATIENT_DIMENSION_KEY
import static org.transmartproject.batch.i2b2.variable.ProviderDimensionI2b2Variable.PROVIDER_DIMENSION_KEY
import static org.transmartproject.batch.i2b2.variable.VisitDimensionI2b2Variable.VISITS_DIMENSION_KEY

/**
 * Stores the mappings between external identifiers and i2b2 identifiers
 * and information about whether the entity is new.
 *
 * Should be job scoped.
 */
@Slf4j
@CompileStatic
class DimensionsStore {
    private final static Set<String> KNOWN_DIMENSION_KEYS = ImmutableSet.of(
            PATIENT_DIMENSION_KEY,
            VISITS_DIMENSION_KEY,
            PROVIDER_DIMENSION_KEY,
    )

    private static final int INTERNAL_ID_INDEX       = 0
    private static final int SAW_DATA_INDEX          = 1
    private static final int KNOWN_AS_EXISTING_INDEX = 2
    private static final int EXTRA_DATA_INDEX        = 3 /* for visits, which need to store the patient id */

    /* this needs to be compact because this class is serialized */
    Table<String /* dimension key */,
            String /* external id */,
            List /* internal id (String), sawData (boolean), knownAsExisting (boolean) */> table =
            TreeBasedTable.create()

    void restore(DimensionsStore other) {
        this.table.putAll(Tables.transformValues(other.table, { List it ->
            Lists.newArrayList(it) // copy lists
        } as Function<List, List>))
    }

    void addExternalId(String dimensionKey,
                       String externalId,
                       boolean sawData) {
        assert dimensionKey in KNOWN_DIMENSION_KEYS

        List entry = table.get(dimensionKey, externalId)
        if (entry) {
            if (sawData && !entry[SAW_DATA_INDEX]) {
                log.debug("Mark external id '$externalId' of type " +
                        "$dimensionKey as having seen data")
                entry[SAW_DATA_INDEX] = true
            }
            return
        }

        log.debug("Adding external id '$externalId' of type $dimensionKey " +
                "(saw data: $sawData)")

        table.put(dimensionKey, externalId, [null, sawData, false])
    }

    void addExtraData(String dimensionKey,
                      String externalId,
                      String extraData) {
        /* only current use for extra data */
        assert dimensionKey == VISITS_DIMENSION_KEY

        List entry = table.get(dimensionKey, externalId)
        if (entry == null) {
            throw new IllegalArgumentException(
                    "Cannot register extra data for non-existent entry")
        }
        def curExtraData = entry[EXTRA_DATA_INDEX]

        if (curExtraData != null && curExtraData != extraData) {
            throw new IllegalArgumentException("Cannot register " +
                    "$dimensionKey/$externalId with different " +
                    "associated data (previously '$curExtraData', " +
                    "now '$extraData'")
        }
        entry[EXTRA_DATA_INDEX] = extraData
    }

    boolean hasDataBeenSeenFor(String dimensionKey, String externalId) {
        List value = table.get(dimensionKey, externalId)
        value ? value[SAW_DATA_INDEX] : false
    }

    void syncWithDatabaseEntry(String dimensionKey,
                               String externalId,
                               String internalId,
                               String extraData) {
        List list = table.get(dimensionKey, externalId)
        assert list != null
        9
        list[INTERNAL_ID_INDEX] = internalId
        list[KNOWN_AS_EXISTING_INDEX] = true
        if (extraData == null) {
            if (list.size() == EXTRA_DATA_INDEX + 1) {
                throw new IllegalStateException("It should never happen " +
                        "that we're registering database entries without " +
                        "extra data and the internal state has extra data")
            }
        } else { // extraData != null
            def curExtraData = list[EXTRA_DATA_INDEX]
            if (curExtraData == null) {
                // validation rules on the mapping file should ensure that
                // a visit dimension always gets registered alongside the
                // corresponding patient dimension. Throw if this invariant is
                // violated
                throw new IllegalStateException("It should never happen " +
                        "that we're registering database entries with " +
                        "extra data and the internal state has no extra data")
            } else if (extraData != curExtraData) {
                throw new IllegalStateException("Mismatch of the data " +
                        "associated with $dimensionKey id $externalId. Data " +
                        "files have $curExtraData, database has $extraData")
            }
            list[EXTRA_DATA_INDEX] = extraData
        }
    }

    void markAsNotInDatabase(String dimensionKey, String externalId) {
        List list = table.get(dimensionKey, externalId)
        assert list != null

        list[INTERNAL_ID_INDEX] = null
        list[KNOWN_AS_EXISTING_INDEX] = false
    }

    String getInternalIdFor(String dimensionKey, String externalId) {
        def data = table.get(dimensionKey, externalId)
        if (data == null) {
            return null
        }

        data[INTERNAL_ID_INDEX] as String
    }

    Iterator<String> getExternalIdIteratorForDimensionKey(String dimensionKey) {
        if (!table.containsRow(dimensionKey)) {
            throw new IllegalArgumentException(
                    "Invalid dimension key: $dimensionKey")
        }

        Iterators.unmodifiableIterator(
                table.row(dimensionKey).keySet().iterator())
    }

    Iterator<DimensionsStoreEntry> getEntriesForDimensionKey(String dimensionKey) {
        Iterators.transform(
                table.row(dimensionKey).entrySet().iterator(),
                { Map.Entry<String, List> entry ->
                    String externalId = entry.key
                    List data = entry.value
                    new DimensionsStoreEntry(
                            externalId: externalId,
                            internalId: data[INTERNAL_ID_INDEX] as String,
                            sawData: (boolean) data[SAW_DATA_INDEX],
                            knownAsExisting: (boolean) data[KNOWN_AS_EXISTING_INDEX],
                            extraData: data[EXTRA_DATA_INDEX],
                    )
                } as Function<Map.Entry<String, List>, DimensionsStoreEntry>)
    }

    void assignInternalId(String dimensionKey,
                          String externalId,
                          String internalId) {
        List data = table.get(dimensionKey, externalId)
        assert data != null
        def current = data[INTERNAL_ID_INDEX]
        if (log.warnEnabled && current != null) {
            log.warn("Replacing interal of $dimensionKey-$externalId from " +
                    "$current to $internalId")
        }
        data[INTERNAL_ID_INDEX] = internalId
    }

    Iterator<String> getExternalIdsWithoutAssociatedInternalIds(
            String dimensionKey) {
        Iterators.transform(
                Iterators.filter(
                        table.row(dimensionKey).entrySet().iterator(),
                        { Map.Entry<String, List> entry ->
                            entry.value /* data */[INTERNAL_ID_INDEX] == null
                        } as Predicate<Map.Entry<String, List>>),
                { Map.Entry<String, List> entry ->
                    entry.key /* external id */
                } as Function<Map.Entry<String, List>, String>)
    }

    LogStatusListener getLogStatusListener(boolean includeKnowAsExisting) {
        new LogStatusListener().with {
            includeKnownAsExisting = includeKnowAsExisting
            it
        }
    }

    private void logStatus(boolean includeKnowAsExisting) {
        log.trace("Table is $table")
        table.rowMap().each { String dimensionKey,
                              Map<String, List<?>> entries ->
            int sawDataCount, knownAsExistingCount
            for (it in entries.values()) {
                if (it[SAW_DATA_INDEX]) {
                    sawDataCount++
                }
                if (it[KNOWN_AS_EXISTING_INDEX]) {
                    knownAsExistingCount++
                }
            }

            log.info("Dimension $dimensionKey: ${entries.size()} entries, " +
                    "$sawDataCount with seen data" +
                    (includeKnowAsExisting ?
                            ", $knownAsExistingCount known as existing"
                            : '') as String)
        }
    }

    class LogStatusListener {

        boolean includeKnownAsExisting

        @AfterStep
        @SuppressWarnings('UnusedMethodParameter')
        ExitStatus afterStep(StepExecution stepExecution) {
            if (log.isInfoEnabled()) {
                logStatus(includeKnownAsExisting)
            }
            stepExecution.exitStatus
        }
    }
}
