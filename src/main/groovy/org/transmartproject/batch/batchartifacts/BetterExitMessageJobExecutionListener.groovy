package org.transmartproject.batch.batchartifacts

import groovy.util.logging.Slf4j
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener

/**
 * Changes the exit message so that it includes the messages of the exception
 * causes.
 */
@Slf4j
class BetterExitMessageJobExecutionListener implements JobExecutionListener {

    @Override
    void beforeJob(JobExecution jobExecution) {
        log.info("Job id is ${jobExecution.jobInstance.id}, " +
                "execution id is ${jobExecution.id}")
    }

    @Override
    void afterJob(JobExecution jobExecution) {
        ExitStatus previousExitStatus = jobExecution.exitStatus

        if (previousExitStatus.exitCode != ExitStatus.FAILED.exitCode) {
            return
        }

        def messages = []
        def seenThrowables = [] as Set
        def allFailures = jobExecution.allFailureExceptions
        while (allFailures) {
            for (Throwable t = allFailures.pop(); t != null; t = t.cause) {
                if (t in seenThrowables) {
                    return
                }
                if (t.hasProperty('nextException')) {
                    allFailures << t.nextException
                }
                seenThrowables << t
                messages << "${t.getClass().simpleName}: ${t.message}"
            }
        }

        if (!messages) {
            return
        }

        jobExecution.exitStatus = new ExitStatus(
                previousExitStatus.exitCode, messages.join("\n"))

        // Also log the new exit description, because the old one was already logged
        // (we don't run early enough)
        log.warn("Exit description: ${jobExecution.exitStatus.exitDescription}")
    }
}
