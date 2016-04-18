package org.transmartproject.batch.batchartifacts

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.springframework.batch.item.validator.ValidationException
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.StaticMessageSource
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import org.transmartproject.batch.test.TestLogUtils

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Test the {@link MessageResolverSpringValidator} class.
 */
class MessageResolverSpringValidatorTests {

    TestBean item = new TestBean()

    private static final List DEFAULT_ERROR_DEFINITIONS = [
            [code: 'globalError1'],
            [code: 'globalError2'],
            [code: 'code11', field: 'doubleField'],
            [code: 'code12', field: 'doubleField'],
            [code: 'code21', field: 'intField'],
            [code: 'code22', field: 'intField'],
    ]

    @Rule
    @SuppressWarnings('PublicInstanceField')
    public final ExpectedException exception = ExpectedException.none()

    static MessageResolverSpringValidator init(Object item,
                                               List errorDefinitions = [:],
                                               Set nonStoppingValidationErrors = [] as Set) {

        Validator validator = new Validator() {
            @Override
            boolean supports(Class<?> clazz) {
                clazz == item.class
            }

            @Override
            void validate(Object target, Errors errors) {
                assert item == target

                errorDefinitions.each {
                    if (it.field) {
                        errors.rejectValue(it.field, it.code)
                    } else {
                        errors.reject(it.code)
                    }
                }
            }
        }

        StaticMessageSource messageSource = new StaticMessageSource()
        errorDefinitions.each {
            messageSource.addMessage(it.code, LocaleContextHolder.locale, "${it.code} message")
        }

        new MessageResolverSpringValidator<TestBean>(validator, messageSource, nonStoppingValidationErrors)
    }

    @Test
    void testSuccessfulValidation() {
        def emptyErrorsList = []
        MessageResolverSpringValidator<TestBean> testee = init(item, emptyErrorsList)

        ListAppender<ILoggingEvent> listAppender = TestLogUtils.initAndAppendListAppenderToTheRootLogger()

        try {
            testee.validate(item)
            assertThat listAppender.list, empty()
        } finally {
            TestLogUtils.removeListAppenderFromTheRootLoggger(listAppender)
        }
    }

    @Test(expected = ValidationException)
    void testThrowException() {
        MessageResolverSpringValidator<TestBean> testee = init(item, DEFAULT_ERROR_DEFINITIONS)

        testee.validate(item)
    }

    @Test
    void testClassIsNotSupported() {
        MessageResolverSpringValidator<TestBean> testee = init(item, DEFAULT_ERROR_DEFINITIONS)

        exception.expect(ValidationException)
        exception.expectMessage(containsString('String class is not supported'))

        testee.validate('string to throw the exception')
    }

    @Test
    void testMessagesAreTakenFromTheSource() {
        MessageResolverSpringValidator<TestBean> testee = init(item, DEFAULT_ERROR_DEFINITIONS)

        exception.expect(ValidationException)
        exception.expectMessage(
                allOf(
                        containsString('code11 message'),
                        containsString('code12 message'),
                        containsString('code21 message'),
                        containsString('code22 message'),
                        containsString('globalError1 message'),
                        containsString('globalError2 message'),
                )
        )

        testee.validate(item)
    }

    @Test
    void testNonStoppingErrorsAreLogged() {
        Set nonStoppingErrors = [
                new ValidationErrorMatcherBean(code: 'globalError1'),
                new ValidationErrorMatcherBean(code: 'globalError2'),
                new ValidationErrorMatcherBean(code: 'code11', field: 'doubleField'),
                new ValidationErrorMatcherBean(code: 'code12', field: 'doubleField'),
                new ValidationErrorMatcherBean(code: 'code21', field: 'intField'),
                new ValidationErrorMatcherBean(code: 'code22', field: 'intField'),
        ]
        MessageResolverSpringValidator<TestBean> testee = init(item,
                DEFAULT_ERROR_DEFINITIONS,
                nonStoppingErrors)
        ListAppender<ILoggingEvent> listAppender = TestLogUtils.initAndAppendListAppenderToTheRootLogger()
        assertThat listAppender.list, empty()

        try {
            testee.validate(item)

            assertThat listAppender.list, contains(
                    allOf(
                            hasProperty('level', equalTo(Level.WARN)),
                            hasProperty('message',
                                    allOf(
                                            containsString('code11 message'),
                                            containsString('code12 message'),
                                            containsString('code21 message'),
                                            containsString('code22 message'),
                                            containsString('globalError1 message'),
                                            containsString('globalError2 message'),
                                    )
                            ),
                    )
            )
        } finally {
            TestLogUtils.removeListAppenderFromTheRootLoggger(listAppender)
        }
    }

    @Test
    void testNonStoppingErrorsNotIncludedToTheExceptionMessage() {
        Set nonStoppingErrors = [
                new ValidationErrorMatcherBean(code: 'globalError2'),
                new ValidationErrorMatcherBean(code: 'code11', field: 'doubleField'),
                new ValidationErrorMatcherBean(code: 'code22', field: 'intField'),
        ]
        MessageResolverSpringValidator<TestBean> testee = init(item,
                DEFAULT_ERROR_DEFINITIONS,
                nonStoppingErrors)

        exception.expect(ValidationException)
        exception.expectMessage(
                allOf(
                        not(containsString('code11 message')),
                        containsString('code12 message'),
                        containsString('code21 message'),
                        not(containsString('code22 message')),
                        containsString('globalError1 message'),
                        not(containsString('globalError2 message')),
                )
        )

        testee.validate(item)
    }

}
