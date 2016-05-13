package org.transmartproject.batch.highdim.assays

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStream
import org.springframework.batch.item.ItemStreamException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator

/**
 * Basic validation {@link MappingFileRow} objects.
 * Because this validator depends on rows on {@link MappingsFileRowStore} which
 * typically are added there by a writer, call this validator from a writer
 * rather than a processor.
 */
@JobScope
@Component
class MappingsFileRowValidator implements Validator, ItemStream {

    private final static String CTX_KEY = 'mappingFileValidator.platform'

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    private String seenPlatform //saved

    boolean supports(Class<?> clazz) {
        clazz == MappingFileRow
    }

    void validate(Object target, Errors errors) {
        assert target instanceof MappingFileRow

        /* mandatory fields */
        if (!target.subjectId) {
            errors.rejectValue 'subjectId', 'required',
                    ['Subject id (code)'] as Object[], null
        }
        if (!target.sampleCd) {
            errors.rejectValue 'sampleCd', 'required',
                    ['Sample code'] as Object[], null
        }
        if (!target.platform) {
            errors.rejectValue 'platform', 'required',
                    ['Platform'] as Object[], null
        }
        if (!target.categoryCd) {
            errors.rejectValue 'categoryCd', 'required',
                    ['Category Code'] as Object[], null
        }

        /* constant field */
        if (seenPlatform) {
            if (seenPlatform != target.platform) {
                errors.rejectValue 'platform', 'valueShouldBeConstant',
                        ['platform', seenPlatform, target.platform] as Object[], null
            }
        } else {
            seenPlatform = target.platform
        }

        if (target.studyId != studyId) {
            errors.rejectValue 'studyId', 'expectedConstant',
                    [studyId, target.studyId] as Object[], null
        }
    }

    void open(ExecutionContext executionContext) throws ItemStreamException {
        seenPlatform = executionContext.getString(CTX_KEY, null)
    }

    void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putString(CTX_KEY, seenPlatform)
    }

    @Override
    @SuppressWarnings('CloseWithoutCloseable')
    void close() throws ItemStreamException {}
}
