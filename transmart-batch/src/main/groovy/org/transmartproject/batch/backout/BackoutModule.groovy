package org.transmartproject.batch.backout

import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.Step
import org.springframework.beans.factory.BeanNameAware
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.core.Ordered

/**
 * Part of the backout job responsible for deleting a certain data type.
 * Implementations should go under the packages of the respective analyses.
 */
abstract class BackoutModule implements Ordered, BeanNameAware {

    public static final int DEFAULT_PRECEDENCE = 0

    public static final ExitStatus FOUND = new ExitStatus('FOUND')

    String beanName

    int order = DEFAULT_PRECEDENCE

    protected String getDataTypeName() {
        if (!(beanName =~ /JobBackoutModule$/).find()) {
            throw new IllegalStateException(
                    "Bean name '$beanName' does not match expected format " +
                            '(does not end in JobBackoutModule)')
        }

        (beanName.replaceFirst(/./) {
            it.toLowerCase(LocaleContextHolder.locale)
        }) - ~/JobBackoutModule$/
    }

    // exit status should be either 'COMPLETED' or 'NOT FOUND'
    abstract Step detectPresenceStep()

    abstract Step deleteDataStep()
}
