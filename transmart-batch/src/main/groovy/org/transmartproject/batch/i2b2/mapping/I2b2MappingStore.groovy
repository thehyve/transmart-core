package org.transmartproject.batch.i2b2.mapping

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.AfterStep
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.validator.ValidationException
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import org.transmartproject.batch.i2b2.variable.*

import static org.transmartproject.batch.i2b2.variable.DimensionExternalIdI2b2Variable.PATIENT_EXTERNAL_ID
import static org.transmartproject.batch.i2b2.variable.DimensionExternalIdI2b2Variable.VISIT_EXTERNAL_ID
import static org.transmartproject.batch.i2b2.variable.ObservationDateI2b2Variable.START_DATE

/**
 * Stores bound {@link I2b2MappingEntry} and {@link objects.
 *
 * Also serves as a point for implementing non-local mapping  entry validation.
 *
 * TODO: implement Errors based validation?
 */
@Component
@JobScope
@TypeChecked
@Slf4j
@SuppressWarnings('DuplicateListLiteral')
class I2b2MappingStore {

    /**
     * Variables that are mandatory when that file has fact variables.
     * The entries for these variables must also be always mandatory (have the
     * mandatory column set to true) even when the variable itself would not
     * be mandatory (i.e., there are no facts in the entry file). See
     * {@link I2b2MappingEntryLocalValidator}.
     */
    private static final Set<I2b2Variable> MANDATORY_FOR_FACTS_VARIABLES = [
            START_DATE, PATIENT_EXTERNAL_ID
    ]  as Set

    private final List<I2b2MappingEntry> entries = []

    private final Table<Tuple /* file and column number */,
                  String /* original */,
                  String /* new value */> wordMappings =
            HashBasedTable.create()

    private Map<Resource, FileFactEntriesSchema> fileFactEntriesSchemas

    @SuppressWarnings('PrivateFieldCouldBeFinal') // codenarc bug
    private int serial = 1

    void leftShift(I2b2MappingEntry entry) {
        if (entry.i2b2Variable == null) {
            throw new IllegalArgumentException(
                    "Unbound mapping entries are not accepted. Got: $entry")
        }

        validate entry

        entry.serial = this.serial++

        entries << entry
    }

    void leftShift(I2b2WordMapping entry) {
        def tuple = fileAndColTuple(entry)
        def previousValue = wordMappings.get(tuple, entry.from)
        if (previousValue) {
            log.warn("Replacing word mapping for $tuple, " +
                    "source value ${entry.from} (replacement was " +
                    "$previousValue, it's not ${entry.to})")
        }

        // normalize nulls to empty strings
        def from = entry.from ?: ''
        def to = entry.to ?: ''
        wordMappings.put tuple, from, to
    }

    String applyWordMapping(I2b2MappingEntry entry, String originalValue) {
        // TODO: this could benefit from caching
        def tuple = new Tuple(entry.filename, entry.columnNumber)
        def mappedValue = wordMappings.get(tuple, originalValue ?: '')
        if (mappedValue == null) {
            originalValue
        } else {
            mappedValue ?: null
        }
    }

    private Tuple fileAndColTuple(I2b2WordMapping wordMapping) {
        new Tuple(wordMapping.filename, wordMapping.columnNumber)
    }

    @SuppressWarnings('UnusedPrivateMethod') // codenarc bug
    private Map<Resource, FileFactEntriesSchema> buildFileFactEntriesSchemas() {
        allEntries.groupBy { I2b2MappingEntry it ->
            it.fileResource
        }.collectEntries { Resource r, List<I2b2MappingEntry> entries ->
            [r, FileFactEntriesSchema.buildFor(entries)]
        } as Map<Resource, FileFactEntriesSchema>
    }

    FileFactEntriesSchema geFileFactEntriesSchemaFor(Resource fileResource) {
        fileFactEntriesSchemas.get(fileResource)
    }

    private void checkForWarningSituations() {
        /* the same concepts and modifiers can show up in different files, but
         * they better resolve to different observation fact primary keys */
        entries.findAll {
            hasFactVariable(it)
        }.groupBy { I2b2MappingEntry it ->
            it.i2b2Variable // concept or modifier; result is var -> list entries
        }.findAll { I2b2Variable var, List<I2b2MappingEntry> entries ->
            entries*.filename.unique().size() > 1
        }.each { I2b2Variable var, List<I2b2MappingEntry> entries ->
            log.warn("The variable $var shows up in more than one " +
                    "file. This is allowed, but the facts must be " +
                    "associated with different combinations of " +
                    "visit, patient, provider and start_date. This " +
                    "WILL NOT be validated; if you violate this " +
                    "requirement, unique constraint violations will " +
                    "show up in the second pass")
        }
    }

    private void fatalGlobalChecks() {
        checkForVariablesRequiredForFacts()
        checkPatientDimVarsAreAccompaniedByPatientId()
        checkVisitDimVarFilesRequirements()
        checkVisitIdIsAcommpaniedByPatientId()

        // provider dimension needs no check
        // the external id can always be generated
    }

    private void checkForVariablesRequiredForFacts() {
        // check that files with facts have the required variables
        // (at least start_date and patient identifier)
        Set<String> allFilesWithFactVars =
                getEntriesWithVariableOfType(ConceptI2b2Variable)*.filename as
                        Set<String>


        MANDATORY_FOR_FACTS_VARIABLES.each { I2b2Variable var ->
            Set<String> filesWithTheMandatoryVar = entries.findAll {
                it.i2b2Variable == var
            }*.filename as Set<String>

            def difference = allFilesWithFactVars - filesWithTheMandatoryVar

            if (difference) {
                throw new ValidationException("The following files have " +
                        "fact variables but don't have the then mandatory " +
                        "variable $var: $difference")
            }
        }
    }

    private void checkPatientDimVarsAreAccompaniedByPatientId() {
        // file w/ patient dimension variables must have a patient external id
        // this variable is always mandatory, so no need to check that it is
        String fileWithPatientDimensionVariables =
                getEntriesWithVariableOfType(PatientDimensionI2b2Variable)
                        .find()?.filename
        if (!fileWithPatientDimensionVariables) {
            log.info('No file with patient dimension variables')
        } else if (!entries.find {
            it.filename == fileWithPatientDimensionVariables &&
                    it.i2b2Variable == PATIENT_EXTERNAL_ID
        }) {
            throw new ValidationException("The file " +
                    "$fileWithPatientDimensionVariables has patient " +
                    "dimension variables. This implies that this file " +
                    "should also have the patient external id " +
                    "variable, but that is not the case")
        }
    }

    private void checkVisitDimVarFilesRequirements() {
        // files with visit dimension variables must have either:
        // a) patient external identifier and start_date (always mandatory)
        //   (for visit auto-generation) or
        // b) mandatory visit dimension identifier and patient external
        //   identifier

        // visit dimension (or any other dimension) variables can be on only
        // one file
        String fileWithVisitDimensionVariables =
                getEntriesWithVariableOfType(VisitDimensionI2b2Variable)
                        .find()?.filename

        if (fileWithVisitDimensionVariables) {
            Collection<I2b2MappingEntry> entriesForFile = entries
                    .findAll { it.filename == fileWithVisitDimensionVariables }

            // test a)
            boolean found = entriesForFile.find {
                it.i2b2Variable == PATIENT_EXTERNAL_ID
            } && entries.find {
                it.variable == START_DATE
            }

            // test b)
            // actually the existence of patient_external_id will be mandated
            // on the test next, but test here so the user knows about all the
            // problems in one go
            found = found || (entriesForFile.find {
                it.i2b2Variable == VISIT_EXTERNAL_ID && it.mandatory
            } && entriesForFile.find {
                it.i2b2Variable == PATIENT_EXTERNAL_ID
            })

            if (!found) {
                throw new ValidationException("The file " +
                        "$fileWithVisitDimensionVariables has visit dimension" +
                        "variables. Then this file should also have a patient " +
                        "dimension identifier and a) a mandatory visit " +
                        "dimension identifier variable or b) a start_date " +
                        "variable, so that the visit dimension external " +
                        "identifier can be auto-generated.")
            }
        }
    }

    private void checkVisitIdIsAcommpaniedByPatientId() {
        // Files with the visit identifier need to have also the patient
        // identifier. This is a crude way to ensure that all visit identifiers
        // we get can be associated with a patient identifier
        Set<String> filesWithVisitDimension = entries.findAll {
            it.i2b2Variable == VISIT_EXTERNAL_ID
        }*.filename as Set<String>

        filesWithVisitDimension.each { String file ->
            if (!entries.find {
                it.filename == file && it.i2b2Variable == PATIENT_EXTERNAL_ID
            }) {
                throw new ValidationException("Files with $VISIT_EXTERNAL_ID " +
                        "mapped must also map $PATIENT_EXTERNAL_ID, but " +
                        "that's not the case with $file")
            }
        }
    }

    class ColumnMappingsPostProcessingListener {

        @AfterStep
        @SuppressWarnings('CatchException')
        ExitStatus afterStep(StepExecution stepExecution) {
            if (stepExecution.exitStatus != ExitStatus.COMPLETED) {
                log.warn("Skipping final validation in order to " +
                        "avoid hiding earlier problems")
                stepExecution.exitStatus
            } else {
                try {
                    /* qualified this necessary to work around Groovy bug */
                    fileFactEntriesSchemas =
                            I2b2MappingStore.this.buildFileFactEntriesSchemas()
                    fileFactEntriesSchemas.values()*.validate()
                    checkForWarningSituations()
                    fatalGlobalChecks()
                    stepExecution.exitStatus
                } catch (ValidationException exc) {
                    log.error 'Failed validation after mapping entries ' +
                            'reading', exc
                    ExitStatus.FAILED
                } catch (Exception e) {
                    log.error 'Unexpected exception after mapping entries ' +
                            'reading', e
                    ExitStatus.FAILED
                }
            }
        }
    }

    def getColumnMappingsPostProcessingListener() {
        new ColumnMappingsPostProcessingListener()
    }

    Collection<I2b2MappingEntry> getAllEntries() {
        entries
    }

    private <T extends I2b2Variable> Collection<I2b2MappingEntry> getEntriesWithVariableOfType(Class<T> type) {
        entries.findAll {
            type.isAssignableFrom(it.i2b2Variable.getClass())
        }
    }

    private void validate(I2b2MappingEntry mappingEntry) {
        def currentEntry = entries.find {
                it.filename == mappingEntry.filename &&
                        it.columnNumber == mappingEntry.columnNumber }
        if (currentEntry && !hasFactVariable(currentEntry)) {
            throw new ValidationException("Multiple mapping entries for the " +
                    "same file and column with non-fact variable: first " +
                    "$currentEntry, then $mappingEntry")
        }


        def var = mappingEntry.i2b2Variable
        validate mappingEntry, var /* multi-dispatch */
    }

    private void validate(I2b2MappingEntry mappingEntry,
                          DimensionExternalIdI2b2Variable var) {
        atMostOnePerFile "external id variable $var", mappingEntry
    }

    private void validate(I2b2MappingEntry mappingEntry,
                          DimensionI2b2Variable var) {
        // all the dimension variables for a certain dimension must be on the
        // same file
        I2b2MappingEntry existing = getEntriesWithVariableOfType(DimensionI2b2Variable)
                .find {
            ((DimensionI2b2Variable) it.i2b2Variable).dimensionTable ==
                    var.dimensionTable &&
                    it.filename != mappingEntry.filename
        }

        if (existing) {
            throw new ValidationException("Mappings for the same dimension " +
                    "table are present in different files. Seen first " +
                    "$existing, then $mappingEntry")
        }
    }

    @SuppressWarnings(['EmptyMethod', 'UnusedPrivateMethodParameter'])
    private void validate(I2b2MappingEntry mappingEntry,
                          ConceptI2b2Variable var /* unused */) {
        // nothing to do; can be repeated
    }

    @SuppressWarnings(['EmptyMethod', 'UnusedPrivateMethodParameter'])
    private void validate(I2b2MappingEntry mappingEntry,
                          ModifierI2b2Variable var /* unused */) {
        // nothing to do; can be repeated
    }

    private void validate(I2b2MappingEntry mappingEntry,
                          ObservationDateI2b2Variable var) {
        atMostOnePerFile("observation fact date ${var.name()}", mappingEntry)
    }

    private void atMostOnePerFile(String desc, I2b2MappingEntry mappingEntry) {
        I2b2MappingEntry existing = entries.find {
            it.i2b2Variable == mappingEntry.i2b2Variable &&
                    it.filename == mappingEntry.filename
        }

        if (existing) {
            throw new ValidationException("Found multiple mappings for the " +
                    "$desc in the same file. Seen " +
                    "first $existing, then $mappingEntry")
        }
    }

    @SuppressWarnings('UnusedPrivateMethodParameter')
    private void validate(I2b2MappingEntry mappingEntry /* unused */,
                          I2b2Variable var) {
        throw new UnsupportedOperationException(
                "Unsupported I2b2Variable subtype: $var")
    }

    private boolean hasFactVariable(I2b2MappingEntry entry) {
        entry.i2b2Variable instanceof ConceptI2b2Variable ||
                entry.i2b2Variable instanceof ModifierI2b2Variable
    }
}
