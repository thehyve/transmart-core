package org.transmartproject.batch.tag

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator

import javax.annotation.Resource

/**
 * Validates {@link Tag} objects.
 */
@Component
@JobScope
class TagValidator implements Validator {

    @Resource
    Validator tagBoundsValidator

    @Override
    boolean supports(Class<?> clazz) {
        clazz == Tag
    }

    @Override
    void validate(Object target, Errors errors) {
        Tag tag = (Tag) target

        if (!tag.conceptFragment) {
            errors.rejectValue 'conceptFragment', 'required',
                    ['concept fragment'] as Object[], null
        }
        if (!tag.tagTitle) {
            errors.rejectValue 'tagTitle', 'required',
                    ['tag title'] as Object[], null
        }
        if (!tag.tagDescription) {
            errors.rejectValue 'tagDescription', 'required',
                    ['tag description'] as Object[], null
        }

        tagBoundsValidator.validate target, errors
    }
}
