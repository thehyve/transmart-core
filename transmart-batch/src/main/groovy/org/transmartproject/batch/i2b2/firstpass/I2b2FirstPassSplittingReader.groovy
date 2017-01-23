package org.transmartproject.batch.i2b2.firstpass

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Slf4j
import org.springframework.batch.core.annotation.OnProcessError
import org.springframework.batch.core.annotation.OnReadError
import org.springframework.batch.item.ItemStreamSupport
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.file.MultiResourceItemReader
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.batch.item.validator.ValidationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.transmartproject.batch.batchartifacts.AbstractSplittingItemReader
import org.transmartproject.batch.i2b2.dimensions.DimensionsStore
import org.transmartproject.batch.i2b2.mapping.FileFactEntriesSchema
import org.transmartproject.batch.i2b2.mapping.I2b2MappingEntry
import org.transmartproject.batch.i2b2.mapping.I2b2MappingStore
import org.transmartproject.batch.i2b2.variable.*

import static org.transmartproject.batch.batchartifacts.AbstractSplittingItemReader.EagerLineListener
import static org.transmartproject.batch.i2b2.variable.PatientDimensionI2b2Variable.PATIENT_DIMENSION_KEY
import static org.transmartproject.batch.i2b2.variable.ProviderDimensionI2b2Variable.PROVIDER_DIMENSION_KEY
import static org.transmartproject.batch.i2b2.variable.VisitDimensionI2b2Variable.VISITS_DIMENSION_KEY

/**
 * Breaks a {@link FieldSet} into {@link I2b2FirstPassDataPoint} variables.
 *
 * Should be job scoped.
 */
@CompileStatic
@Slf4j
class I2b2FirstPassSplittingReader extends
        AbstractSplittingItemReader<I2b2FirstPassDataPoint> {

    @Autowired
    private I2b2MappingStore mappingStore

    @Autowired
    private ExternalIdentifierGenerator identifierGenerator

    @Autowired
    private DimensionsStore dimensionsStore

    // don't need saving
    private Resource lastSeenResource

    // set iif lastSeenResource is set:
    private NavigableMap<Integer /* col number - 1 (0-based) */, I2b2MappingEntry> cachedEntries
    private Set<I2b2MappingEntry> eidVariableEntries
    private FileFactEntriesSchema fileFactEntriesSchema
    private boolean hasFacts
    private boolean hasVisitsVariables
    private Integer startDateColumnNumber // 1-based
    private boolean hasProviderVariables

    I2b2FirstPassSplittingReader() {
        eagerLineListener = new RegisterDimensionsRowListener()
    }

    void setDelegate(MultiResourceItemReader<FieldSet> delegate) {
        super.delegate = delegate
    }

    @Override
    @SuppressWarnings('CatchException')
    protected FieldSet fetchNextDelegateLine() {
        FieldSet result
        Exception e
        boolean changedResource

        try {
            result = super.fetchNextDelegateLine()
        } catch (Exception exc) {
            e = exc
        }

        def currentResource = typedDelegate.currentResource
        assert result == null || currentResource != null
        changedResource = currentResource != lastSeenResource
        if (changedResource && currentResource != null) {
            log.info "Started reading file $typedDelegate.currentResource"
            // reset our state, so we count the lines correctly
            // this was, when we rethrow the exception and it's caught
            // inside wrappedDelegateLineFetch(), the position can be
            // correct
            upstreamPos = 1
            lastSeenResource = typedDelegate.currentResource
        } else if (currentResource == null) {
            log.debug 'MultiResourceItemReader has null resource set'
        }

        if (e) {
            throw e
        }

        if (result == null) {
            return
        }

        if (changedResource) {
            rebuildVariableCache()
            if (result && result.fieldCount < cachedEntries.lastKey()) {
                throw new ValidationException("Expected $lastSeenResource to " +
                        "have at least ${cachedEntries.lastKey()} columns; " +
                        "saw only ${result.fieldCount}")
            }
        }

        /* move position to first mapped field on the field set */
        def boxedPosition = cachedEntries.ceilingKey(0) // position's a primitive
        assert boxedPosition != null
        position = boxedPosition

        result
    }

    private void rebuildVariableCache() {
        def cachedEntriesLocal = mappingStore.allEntries.findAll {
            it.fileResource == typedDelegate.currentResource
        }.collectEntries { I2b2MappingEntry it ->
            [it.columnNumber - 1 /* make 0-based */, it]
        }
        cachedEntries = new TreeMap<Integer, I2b2MappingEntry>(
                cachedEntriesLocal)
        eidVariableEntries = cachedEntries.values().findAll {
            it.i2b2Variable instanceof DimensionExternalIdI2b2Variable
        } as Set
        fileFactEntriesSchema =
                mappingStore.geFileFactEntriesSchemaFor(lastSeenResource)
        hasFacts = cachedEntries.values().find {
            it.i2b2Variable instanceof ConceptI2b2Variable
        } as boolean
        hasVisitsVariables = cachedEntries.values().find {
            it.i2b2Variable instanceof VisitDimensionI2b2Variable
        } as boolean
        hasProviderVariables = cachedEntries.values().find {
            it.i2b2Variable instanceof ProviderDimensionI2b2Variable
        } as boolean
        startDateColumnNumber = cachedEntries.values().find {
            it.i2b2Variable == ObservationDateI2b2Variable.START_DATE
        }?.columnNumber

        log.debug("Entries: $cachedEntries, external id variables: " +
                "$eidVariableEntries, has facts: $hasFacts")

        assert !cachedEntries.empty:
                "there are entries for ${typedDelegate.currentResource}"
    }

    @Override
    protected I2b2FirstPassDataPoint doRead() {
        I2b2MappingEntry entry = cachedEntries[position]
        assert entry != null: "There is an entry for position $position"

        String data = currentFieldSet.readString(position) ?: null
        data = mappingStore.applyWordMapping(entry, data)
        def result = new I2b2FirstPassDataPoint(
                entry: entry,
                data: data,
                resource: lastSeenResource,
                /* upstreamPos is the #lines read from upstream
                   +1 because of header. This *should* be the same as
                   delegateDelegate.lineCount (which is private) */
                line: upstreamPos + 1,)

        position = cachedEntries.ceilingKey(position + 1) ?:
                currentFieldSet.fieldCount // move till end if no more mapped cols
        // because parent class increments it after calling this method
        // TODO: put position incrementing logic responsibility in subclasses?
        position--

        result
    }

    private MultiResourceItemReader<FieldSet> getTypedDelegate() {
        (MultiResourceItemReader<FieldSet>) delegate
    }

    @OnReadError
    void onReadError(Exception ex) {
        log.info("Dumping reader state after read error $ex")
        dumpReaderState()
    }

    @OnProcessError
    void onProcessError(I2b2FirstPassDataPoint item, Exception ex) {
        log.info("Dumping reader state after process error $ex with item $item")
        dumpReaderState()
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void dumpReaderState() {
        // private read here... well this is only for troubleshooting right
        FlatFileItemReader delegateDelegate = typedDelegate.delegate

        log.warn("First pass reader state:\n" +
                "(if error was on a processor or writer, this is the state " +
                "after the last read chunk)\n" +
                "current resource: ${typedDelegate.currentResource}\n" +
                "line position upstream: ${delegateDelegate.lineCount}\n" +
                "lines read from upstream (minus header): $upstreamPos\n" +
                "current field set: $currentFieldSet\n" +
                "current field set position: $position\n" +
                "entries: $cachedEntries\n" +
                "identifier entries: $eidVariableEntries\n" +
                "hasFacts: $hasFacts")
    }

    class RegisterDimensionsRowListener extends ItemStreamSupport
            implements EagerLineListener<I2b2FirstPassDataPoint> {

        @Override
        void onLine(FieldSet fieldSet, Collection<I2b2FirstPassDataPoint> keptItems) {
            Map<String, String> externalIds = getExternalIds(fieldSet)
            Set<String> dimensionsWithData = getDimensionsWithData(keptItems)

            checkForRepeatedDimensionData(externalIds, dimensionsWithData)
            checkForModifierDataWithoutConceptData(fieldSet)
            registerExternalIds(externalIds, dimensionsWithData)
        }

        private void checkForRepeatedDimensionData(Map<String, String> externalIds,
                                                   Set<String> dimensionsWithData) {
            dimensionsWithData.each { String dimensionKey ->
                def externalId = externalIds[dimensionKey]
                assert externalId != null
                if (dimensionsStore.hasDataBeenSeenFor(
                        dimensionKey, externalId)) {
                    throw new ValidationException("Saw details more than " +
                            "once for dimension $dimensionKey, key $externalId")
                }
            }
        }

        private void checkForModifierDataWithoutConceptData(FieldSet fs) {
            fileFactEntriesSchema.each { I2b2MappingEntry conceptEntry,
                                         List<I2b2MappingEntry> modifierEntries ->
                if (dataForEntry(fs, conceptEntry) == null) {
                    // no data for concept
                    // see if one of the dimensions has data
                    I2b2MappingEntry modifierEntryWithData =
                            modifierEntries.find { dataForEntry(fs, it) != null }
                    if (modifierEntryWithData) {
                        throw new ValidationException("No data for entry " +
                                "$conceptEntry, yet found data for one of " +
                                "its modifiers: $modifierEntryWithData (data " +
                                "was ${dataForEntry(fs, modifierEntryWithData)})")
                    }
                }
            }
        }

        private void registerExternalIds(Map<String, String> externalIds,
                                         Set<String> dimensionsWithData) {
            externalIds.each { String dimensionKey, String id ->
                dimensionsStore.addExternalId(
                        dimensionKey,
                        id,
                        dimensionKey in dimensionsWithData)
            }
            if (externalIds[VISITS_DIMENSION_KEY] != null) {
                // this should've been ensured by the mapping validations
                assert externalIds[PATIENT_DIMENSION_KEY] != null
                dimensionsStore.addExtraData(
                        VISITS_DIMENSION_KEY,
                        externalIds[VISITS_DIMENSION_KEY],
                        externalIds[PATIENT_DIMENSION_KEY])
            }
        }

        private Map<String /* dimension key */, String /* identifier */> getExternalIds(FieldSet fieldSet) {
            Map<String, String> externalIds = [:]
            eidVariableEntries.each {
                String data = dataForEntry(fieldSet, it)
                if (data) {
                    DimensionExternalIdI2b2Variable var =
                            (DimensionExternalIdI2b2Variable) it.i2b2Variable
                    externalIds[var.dimensionKey] = data
                }
            }

            if (externalIds[VISITS_DIMENSION_KEY] == null &&
                    (hasFacts || hasVisitsVariables)) {
                generateVisitIdentifier(fieldSet, externalIds)
            }

            if (externalIds[PROVIDER_DIMENSION_KEY] == null &&
                    (hasFacts || hasProviderVariables)) {
                externalIds[PROVIDER_DIMENSION_KEY] =
                        identifierGenerator.generateProviderIdentifier()
            }

            externalIds
        }

        private String dataForEntry(FieldSet fs, I2b2MappingEntry entry) {
            fs.readString(entry.columnNumber - 1) ?: null
        }

        private Set<String /* dimension key */> getDimensionsWithData(
                Collection<I2b2FirstPassDataPoint> items) {
            items.findAll {
                it.data &&
                        it.entry.i2b2Variable instanceof DimensionI2b2Variable
            }.collect {
                ((DimensionI2b2Variable) it.entry.i2b2Variable).dimensionKey
            } as Set<String>
        }

        private void generateVisitIdentifier(FieldSet fieldSet,
                                             Map<String, String> externalIds) {
            // generate visits identifier
            assert startDateColumnNumber != null
            String dateString = fieldSet.readString(
                    startDateColumnNumber - 1) ?: null
            assert (dateString as boolean):
                    "we have a date when generating the visit identifier"
            assert externalIds[PATIENT_DIMENSION_KEY] != null:
                    "we have a patient id when generating the visit id"

            try {
                externalIds[VISITS_DIMENSION_KEY] =
                        identifierGenerator.generateVisitExternalIdentifier(
                                dateString, externalIds[PATIENT_DIMENSION_KEY])
            } catch (IllegalArgumentException iae) {
                throw new ValidationException("The start date $dateString " +
                        "does not conform to the configured format", iae)
            }
        }
    }
}
