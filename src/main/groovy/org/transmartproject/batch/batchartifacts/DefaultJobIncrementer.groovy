package org.transmartproject.batch.batchartifacts

import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.JobParametersIncrementer

/**
 * JobParametersIncrementer impl that makes sure we increment the run.id (resuming from last value)</br>
 * This allows us to re-run a job for the same set of parameters.
 */
class DefaultJobIncrementer implements JobParametersIncrementer {

    @Override
    JobParameters getNext(JobParameters parameters) {
        if (parameters == null || parameters.isEmpty()) {
            new JobParametersBuilder().
                    addLong("run.id", 1L).
                    toJobParameters()
        } else {
            long id = parameters.getLong("run.id", 1L) + 1
            new JobParametersBuilder().
                    addLong("run.id", id).
                    toJobParameters()
        }
    }
}
