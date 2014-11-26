package org.transmartproject.batch.batchartifacts

import groovy.transform.TypeChecked
import org.springframework.batch.item.validator.ValidationException
import org.springframework.batch.item.validator.Validator
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.BindException
import org.springframework.validation.Errors
import org.springframework.validation.ObjectError

/**
 * Like SpringValidator, but resolving messages.
 *
 * Not a bean.
 */
class MessageResolverSpringValidator<T> implements Validator<T> {

    private final org.springframework.validation.Validator validator

    private final MessageSource validationMessageSource

    MessageResolverSpringValidator(org.springframework.validation.Validator validator,
                                   MessageSource validationMessageSource) {
        this.validator = validator
        this.validationMessageSource = validationMessageSource
    }

    @Override
    void validate(T item) throws ValidationException {

        if (!validator.supports(item.getClass())) {
            throw new ValidationException("Validation failed for " + item + ": " + item.getClass().name
                    + " class is not supported by validator.")
        }

        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(item, "item")

        validator.validate(item, errors)

        if (errors.hasErrors()) {
            throw new ValidationException("Validation failed for " + item + ": " +
                    errorsToString(errors), new BindException(errors))
        }
    }

    private String errorsToString(Errors errors) {
        StringBuffer builder = new StringBuffer()

        appendErrorCollection errors.fieldErrors, builder
        appendErrorCollection errors.globalErrors, builder

        builder.toString()
    }

    @TypeChecked
    private void appendErrorCollection(Collection<? extends ObjectError> collection,
                                       StringBuffer builder) {
        collection.each { ObjectError value ->
            builder.append '\n'
            builder.append validationMessageSource.getMessage(
                    value, LocaleContextHolder.locale)
        }
    }
}
