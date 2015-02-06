package org.transmartproject.batch.batchartifacts

import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.JobParametersIncrementer
import org.springframework.batch.core.launch.support.RunIdIncrementer

/**
 * JobParametersIncrementer that adds a parameter with the date.
 * We do not use {@link RunIdIncrementer} because it depends on the jobs being
 * consistently executed with -n. The parameters passed to getNext() are the
 * parameter of the last created job instance. But, for instance, if the first
 * job instance was run with -n (and therefore got run.id=1), then one is run
 * *without* -n (and get no run.id) parameter, at this point the latest job
 * instance will be the one without run.id. A job then started with -n, will get
 * run.id=1 and the first run will be resumed.
 */
class DefaultJobIncrementer implements JobParametersIncrementer {

    @Override
    JobParameters getNext(JobParameters parameters) {
        new JobParametersBuilder().
                addDate("run.date", new Date()).
                toJobParameters()
    }
}
