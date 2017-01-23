package org.transmartproject.batch.i2b2.mapping

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import org.transmartproject.batch.i2b2.variable.I2b2Variable
import org.transmartproject.batch.support.ConfigurableLengthSemanticsTrait

import java.nio.file.Files
import java.nio.file.Path

import static org.transmartproject.batch.i2b2.mapping.I2b2MappingStore.MANDATORY_FOR_FACTS_VARIABLES

/**
 * Validation of mapping entries that doesn't require any state.
 */
@Component
@Scope('singleton')
class I2b2MappingEntryLocalValidator implements Validator, ConfigurableLengthSemanticsTrait {

    public static final int MAX_SIZE_OF_UNIT = 50

    @Override
    boolean supports(Class<?> clazz) {
        clazz == I2b2MappingEntry
    }

    @Override
    void validate(Object target, Errors errors) {
        I2b2MappingEntry entry = (I2b2MappingEntry) target

        if (!entry.i2b2Variable) {
            throw new IllegalArgumentException("This validator should be " +
                    "called on entries with i2b2Variable set")
        }

        I2b2Variable var = entry.i2b2Variable

        if (!entry.filename) {
            errors.rejectValue(
                    'filename', 'required', ['filename'] as Object[], null)
        } else {
            Path path = entry.fileResource.file.toPath()
            if (!Files.isReadable(path) || !Files.isRegularFile(path)) {
                errors.rejectValue(
                        'filename', 'notReadableFile', [path] as Object[], null)
            }
        }

        if (!entry.columnNumber) {
            errors.rejectValue('columnNumber', 'required',
                    ['columnNumber'] as Object[], null)
        } else if (entry.columnNumber < 1) {
            errors.rejectValue(
                    'columnNumber', 'mustBePositive', [] as Object[], null)
        }

        // some variables (the mandatory variables) cannot mandatory = false
        if (var in MANDATORY_FOR_FACTS_VARIABLES && !entry.mandatory) {
            errors.rejectValue('mandatory', 'varMustBeMandatory',
                    [var] as Object[], null)
        }

        if (!var.admittingFactValues && entry.type != null) {
            errors.rejectValue('type', 'varDoesNotAdmitType',
                    [var, entry.type] as Object[], null)
        }

        if (!var.admittingFactValues && entry.unit != null) {
            errors.rejectValue('unit', 'varDoesNotAdmitUnit',
                    [var, entry.unit] as Object[], null)
        }

        if (var.admittingFactValues && entry.type == null) {
            errors.rejectValue('type', 'varRequiresType',
                    [var] as Object[], null)
        }

        if (entry.type && entry.dataType == null) {
            errors.rejectValue('type', 'invalidDataType',
                    [entry.type] as Object[], null)
        }

        if (entry.unit != null) {
            int lengthOfUnit = lengthOf(entry.unit)
            if (entry.unit.size() > MAX_SIZE_OF_UNIT) {
                errors.rejectValue 'unit', 'maxSizeExceeded',
                        ['unit', lengthOfUnit, MAX_SIZE_OF_UNIT] as Object[],
                        null
            }
        }
    }
}
