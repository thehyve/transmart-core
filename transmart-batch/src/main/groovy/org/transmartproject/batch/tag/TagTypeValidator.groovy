package org.transmartproject.batch.tag

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator

/**
 * Validates {@link TagType} objects.
 */
@Component
@JobScope
class TagTypeValidator implements Validator {

    @Override
    boolean supports(Class<?> clazz) {
        clazz == TagType
    }

    @Override
    void validate(Object target, Errors errors) {
        TagType tagType = (TagType) target

        if (!tagType.nodeType) {
            errors.rejectValue 'nodeType', 'required',
                    ['nodeType'] as Object[], null
        }
        if (!tagType.title) {
            errors.rejectValue 'title', 'required',
                    ['title'] as Object[], null
        }
        if (!tagType.valueType) {
            errors.rejectValue 'valueType', 'required',
                    ['valueType'] as Object[], null
        }
    }
}
