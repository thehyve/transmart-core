package org.transmartproject.batch.junit

import groovy.util.logging.Slf4j
import org.junit.rules.ExternalResource
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.launch.support.CommandLineJobRunner
import org.springframework.batch.core.launch.support.SystemExiter
import org.transmartproject.batch.startup.RunJob

/**
 * This is a class rule.
 */
@Slf4j
class RunJobRule extends ExternalResource {

    private final String studyId
    private final String dataType

    RunJobRule(String studyOrPlatformId, String dataType) {
        this.studyId = studyOrPlatformId
        this.dataType = dataType
    }

    JobParameters jobParameters
    String jobName
    Date jobEndDate

    protected void before() throws Throwable {
        CommandLineJobRunner.presetSystemExiter({ int it -> } as SystemExiter)
        def runJob = RunJob.createInstance(
                '-p', "studies/$studyId/${dataType}.params" as String)
        runJob.run()

        jobParameters = runJob.finalJobParameters
        jobName = runJob.jobName
        jobEndDate = new Date()
    }
}
