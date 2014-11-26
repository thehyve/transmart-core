package org.transmartproject.batch.junit

import org.junit.rules.ExternalResource
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.launch.support.CommandLineJobRunner
import org.springframework.batch.core.launch.support.SystemExiter
import org.transmartproject.batch.startup.RunJob

/**
 * This is a class rule.
 */
class RunJobRule extends ExternalResource {

    String studyId

    RunJobRule(String studyOrPlatformId) {
        this.studyId = studyOrPlatformId
    }

    JobParameters jobParameters
    Date jobEndDate

    protected void before() throws Throwable {
        CommandLineJobRunner.presetSystemExiter({ int it -> } as SystemExiter)
        def runJob = RunJob.createInstance(
                '-p', 'studies/' + studyId + '/annotation.params')
        runJob.run()

        jobParameters = runJob.finalJobParameters
        jobEndDate = new Date()
    }
}
