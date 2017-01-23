package org.transmartproject.batch.i2b2.secondpass

import com.google.common.collect.ImmutableMap
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStreamException
import org.springframework.batch.item.file.MultiResourceItemReader
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.transmartproject.batch.i2b2.dimensions.DimensionsStore
import org.transmartproject.batch.i2b2.fact.FactFactory
import org.transmartproject.batch.i2b2.fact.FactGroup
import org.transmartproject.batch.i2b2.fact.FactValue
import org.transmartproject.batch.i2b2.mapping.FileFactEntriesSchema
import org.transmartproject.batch.i2b2.mapping.I2b2MappingEntry
import org.transmartproject.batch.i2b2.mapping.I2b2MappingStore
import org.transmartproject.batch.i2b2.misc.DateConverter
import org.transmartproject.batch.i2b2.variable.*

import static org.transmartproject.batch.i2b2.variable.DimensionExternalIdI2b2Variable.*
import static org.transmartproject.batch.i2b2.variable.DimensionI2b2Variable.I2b2DimensionVariableType.DATE
import static org.transmartproject.batch.i2b2.variable.DimensionI2b2Variable.I2b2DimensionVariableType.INTEGER
import static org.transmartproject.batch.i2b2.variable.ObservationDateI2b2Variable.END_DATE
import static org.transmartproject.batch.i2b2.variable.ObservationDateI2b2Variable.START_DATE
import static org.transmartproject.batch.i2b2.variable.PatientDimensionI2b2Variable.PATIENT_DIMENSION_KEY
import static org.transmartproject.batch.i2b2.variable.VisitDimensionI2b2Variable.VISITS_DIMENSION_KEY

/**
 * Second pass inner reader (on the outside there's a
 * {@link MultiResourceItemReader}).
 *
 * Unlike in the first pass, we don't split rows.
 *
 * Should be at least job-scoped.
 */
@CompileStatic
@Slf4j
class I2b2SecondPassInnerReader implements ResourceAwareItemReaderItemStream<I2b2SecondPassRow> {

    @Autowired
    private I2b2MappingStore mappingStore

    @Autowired
    private DimensionsStore dimensionsStore

    @Autowired
    private ExternalIdentifierGenerator externalIdentifierGenerator

    @Autowired
    private DateConverter dateConverter

    @Autowired
    private FactFactory factFactory

    ResourceAwareItemReaderItemStream<FieldSet> delegate

    private Resource resource

    private I2b2MappingEntry patientEidEntry,
                             visitEidEntry,
                             providerEidEntry,
                             startDateEntry,
                             endDateEntry

    private Collection<I2b2MappingEntry> patientDimensionEntries
    private Collection<I2b2MappingEntry> visitDimensionEntries
    private Collection<I2b2MappingEntry> providerDimensionEntries

    private FileFactEntriesSchema fileFactEntriesSchema

    private Boolean fileHasFactEntries

    @Override
    void open(ExecutionContext executionContext) throws ItemStreamException {
        log.info("Opening for resource $resource")

        Collection<I2b2MappingEntry> entries = mappingStore.allEntries.findAll {
            it.fileResource == resource
        } // entries for this file

        patientEidEntry = entries.find { it.i2b2Variable == PATIENT_EXTERNAL_ID }
        visitEidEntry = entries.find { it.i2b2Variable == VISIT_EXTERNAL_ID }
        providerEidEntry = entries.find { it.i2b2Variable == PROVIDER_EXTERNAL_ID }
        startDateEntry = entries.find { it.i2b2Variable == START_DATE }
        endDateEntry = entries.find { it.i2b2Variable == END_DATE }
        patientDimensionEntries = entries.findAll {
            it.i2b2Variable instanceof PatientDimensionI2b2Variable
        }
        visitDimensionEntries = entries.findAll {
            it.i2b2Variable instanceof VisitDimensionI2b2Variable
        }
        providerDimensionEntries = entries.findAll {
            it.i2b2Variable instanceof ProviderDimensionI2b2Variable
        }

        fileFactEntriesSchema = mappingStore.geFileFactEntriesSchemaFor resource
        fileHasFactEntries = !fileFactEntriesSchema.empty

        log.trace("Calling open on the delegate")
        delegate.open(executionContext)

        log.debug("State is ${this.properties}")
    }

    @Override
    void update(ExecutionContext executionContext) throws ItemStreamException {
        log.trace('Calling update on the delegate')
        delegate.update(executionContext)
    }

    @Override
    @SuppressWarnings('CloseWithoutCloseable')
    void close() throws Exception {
        log.debug("Called close for resource $resource")

        log.trace('Calling close on the delegate')
        delegate.close()
    }

    @Override
    void setResource(Resource resource) {
        this.resource = resource
        delegate.resource = resource
    }

    @Override
    I2b2SecondPassRow read() {
        FieldSet fs = delegate.read()
        if (fs == null) {
            log.debug('Delegate returned null')
            return null
        }

        def r = new I2b2SecondPassRow()

        // dimension ids and dates
        def patientEid = getValue(fs, patientEidEntry)
        def visitEid = getValue(fs, visitEidEntry)
        r.providerId = getValue(fs, providerEidEntry)

        def startDateString = getValue(fs, startDateEntry)
        if (startDateString) {
            r.startDate = dateConverter.parse(startDateString)
        }
        def endDateString = getValue(fs, endDateEntry)
        if (endDateString) {
            r.endDate = dateConverter.parse(endDateString)
        }

        if (fileHasFactEntries && visitEid == null) {
            assert startDateString != null
            assert patientEid != null
            visitEid = externalIdentifierGenerator
                    .generateVisitExternalIdentifier(startDateString, patientEid)
        }
        if (fileHasFactEntries && r.providerId == null) {
            r.providerId = externalIdentifierGenerator
                    .generateProviderIdentifier()
        }

        if (patientEid) {
            r.patientNum = dimensionsStore
                    .getInternalIdFor(PATIENT_DIMENSION_KEY, patientEid) as Long
        }
        if (visitEid) {
            r.encounterNum = dimensionsStore
                    .getInternalIdFor(VISITS_DIMENSION_KEY, visitEid) as Long
        }

        // dimension values
        r.patientDimensionValues = this.<PatientDimensionI2b2Variable> getDimensionValues(
                patientDimensionEntries, fs)
        r.visitDimensionValues = this.<VisitDimensionI2b2Variable> getDimensionValues(
                visitDimensionEntries, fs)
        r.providerDimensionValues = this.<ProviderDimensionI2b2Variable> getDimensionValues(
                providerDimensionEntries, fs)

        // facts
        r.factGroups = []
        // use variable here; not entry! Some entries share equal concept variables
        Map<ConceptI2b2Variable, Integer> instanceNumCounter = [:]

        fileFactEntriesSchema.each { I2b2MappingEntry conceptEntry,
                                     List<I2b2MappingEntry> modifierEntries ->
            ConceptI2b2Variable conceptVariable =
                    (ConceptI2b2Variable) conceptEntry.i2b2Variable

            String conceptValue = getValue(fs, conceptEntry)
            if (!conceptValue) {
                return
            }

            def instanceNum = instanceNumCounter[conceptVariable] ?: 1
            instanceNumCounter[conceptVariable] = instanceNum + 1

            ImmutableMap.Builder<I2b2MappingEntry, FactValue> modifiersFactsBuilder =
                    ImmutableMap.builder()

            modifierEntries.each { I2b2MappingEntry modifierEntry ->
                String modifierValue = getValue(fs, modifierEntry)
                if (!modifierValue) {
                    return
                }
                modifiersFactsBuilder.put(
                        modifierEntry,
                        factFactory.create(modifierEntry.dataType, modifierValue))
            }

            def factGroup = new FactGroup(
                    instanceNum: instanceNum,
                    conceptEntry: conceptEntry,
                    conceptFact: factFactory.create(
                            conceptEntry.dataType, conceptValue),
                    modifierFacts: modifiersFactsBuilder.build())
            r.factGroups << factGroup
        }

        r
    }

    private <T extends DimensionI2b2Variable> Map<T, Object> getDimensionValues(
            Collection<I2b2MappingEntry> dimensionValueEntries,
            FieldSet fs) {
        dimensionValueEntries.collectEntries { I2b2MappingEntry entry ->
            DimensionI2b2Variable var = (DimensionI2b2Variable) entry.i2b2Variable

            Object v = getValue(fs, entry)
            if (var.variableType == INTEGER && v != null) {
                v = new BigInteger(v)
            }
            if (var.variableType == DATE && v != null) {
                v = dateConverter.parse((String) v)
            }

            [entry.i2b2Variable, v]
        }
    }

    private static String getOriginalValue(FieldSet fieldSet, I2b2MappingEntry entry) {
        if (entry == null) {
            return null
        }
        String stringValue = fieldSet.readString(entry.columnNumber - 1)
        stringValue ?: null
    }

    private String getValue(FieldSet fieldSet, I2b2MappingEntry entry) {
        if (entry == null) {
            return null
        }
        String originalValue = getOriginalValue fieldSet, entry
        String newValue = mappingStore.applyWordMapping(entry, originalValue)
        if (log.isTraceEnabled()) {
            log.trace "For entry $entry, replaced $originalValue with $newValue"
        }

        newValue
    }
}
