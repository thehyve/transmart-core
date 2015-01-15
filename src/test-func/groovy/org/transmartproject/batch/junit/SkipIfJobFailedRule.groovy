package org.transmartproject.batch.junit

import groovy.transform.Canonical
import org.junit.internal.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.repository.JobRepository

import java.util.concurrent.TimeUnit

import static org.hamcrest.Matchers.*
import static uk.co.it.modular.hamcrest.date.DateMatchers.within

/**
 * To be used together with {@link RunJobRule}.
 */
@Canonical
class SkipIfJobFailedRule implements TestRule {

    // we need these three to be able to fetch the execution
    Closure<JobRepository> jobRepositoryProvider
    RunJobRule runJobRule

    Statement apply(final Statement base, final Description description) {
        if (description.annotations.any { it.annotationType() == NoSkipIfJobFailed }) {
            return base
        }

        { ->
            if (!jobCompletedSuccessFully) {
                throw new AssumptionViolatedException('The job completed successfully')
            }

            base.evaluate()
        } as Statement
    }

    boolean isJobCompletedSuccessFully() {
        def execution = jobRepositoryProvider().getLastJobExecution(
                runJobRule.jobName, runJobRule.jobParameters)

        assert execution != null: 'Could not find execution'

        allOf(
                is(notNullValue()),
                within(30, TimeUnit.SECONDS, runJobRule.jobEndDate)
        ).matches(execution.endTime) &&
                equalTo(BatchStatus.COMPLETED).matches(execution.status)
    }
}
