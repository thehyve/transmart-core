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

    private final List<String> extraArguments
    private final String paramsFilePath

    RunJobRule(String studyOrPlatformId,
               String dataType,
               List<String> extraArguments = []) {
        this("studies/${studyOrPlatformId}/${dataType}.params", extraArguments)
    }

    RunJobRule(String paramsFilePath,
               List<String> extraArguments = []) {
        this.paramsFilePath = paramsFilePath
        this.extraArguments = extraArguments
    }

    JobParameters jobParameters
    String jobName
    Date jobEndDate
    int result = -1

    protected void before() throws Throwable {
        CommandLineJobRunner.presetSystemExiter({ int it -> } as SystemExiter)
        def runJob = RunJob.createInstance(
                '-p', paramsFilePath,
                *extraArguments)
        result = runJob.run()

        jobParameters = runJob.finalJobParameters
        jobName = runJob.jobName
        jobEndDate = new Date()
    }
}
