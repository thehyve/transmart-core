package org.transmartproject.batch.batchartifacts

import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import org.springframework.batch.item.validator.ValidationException
import org.springframework.batch.item.validator.Validator
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.BindException
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError

/**
 * Like SpringValidator, but resolving messages and introduces notion of non-stopping errors (logged only).
 *
 * Not a bean.
 */
@Slf4j
class MessageResolverSpringValidator<T> implements Validator<T> {

    public static final String ERRORS_DIVIDER = '\n'
    private final org.springframework.validation.Validator validator

    private final MessageSource validationMessageSource

    private final Set<ValidationErrorMatcherBean> nonStoppingValidationErrors

    MessageResolverSpringValidator(org.springframework.validation.Validator validator,
                                   MessageSource validationMessageSource,
                                   Set<ValidationErrorMatcherBean> nonStoppingValidationErrors = [] as Set) {
        this.validator = validator
        this.validationMessageSource = validationMessageSource
        this.nonStoppingValidationErrors = nonStoppingValidationErrors
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
            reportErrors(errors)
        }
    }

    private void reportErrors(BeanPropertyBindingResult errors) {
        String startErrorMessage = "Validation failed for ${errors.target}: "

        def (stoppingGlobalErrors, nonStoppingGlobalErrors) = splitErrorsByImpact(errors.globalErrors)
        def (stoppingFieldErrors, nonStoppingFieldErrors) = splitErrorsByImpact(errors.fieldErrors)

        def allNonStoppingErrors = nonStoppingGlobalErrors + nonStoppingFieldErrors
        if (allNonStoppingErrors) {
            log.warn startErrorMessage + collectMessages(allNonStoppingErrors).join(ERRORS_DIVIDER)
        }

        def allStoppingErrors = stoppingGlobalErrors + stoppingFieldErrors
        if (allStoppingErrors) {
            throw new ValidationException(startErrorMessage +
                    collectMessages(allStoppingErrors).join(ERRORS_DIVIDER),
                    new BindException(errors))
        }
    }

    private splitErrorsByImpact(List<? extends ObjectError> errors) {
        errors.split { isStoppingError(it) }
    }

    private boolean isStoppingError(ObjectError error) {
        if (!nonStoppingValidationErrors) {
            return true
        }

        def errorMatcherBean = new ValidationErrorMatcherBean(code: error.code)
        if (error instanceof FieldError) {
            errorMatcherBean.field = error.field
        }

        !(errorMatcherBean in nonStoppingValidationErrors)
    }

    @TypeChecked
    private List<String> collectMessages(List<? extends ObjectError> errors) {
        errors.collect { ObjectError value ->
            validationMessageSource.getMessage(value, LocaleContextHolder.locale)
        }
    }
}
