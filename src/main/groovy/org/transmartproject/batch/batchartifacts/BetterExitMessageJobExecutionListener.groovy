package org.transmartproject.batch.batchartifacts

import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener

/**
 * Changes the exit message so that it includes the messages of the exception
 * causes.
 */
class BetterExitMessageJobExecutionListener implements JobExecutionListener{

    @Override
    void beforeJob(JobExecution jobExecution) { }

    @Override
    void afterJob(JobExecution jobExecution) {
        ExitStatus previousExitStatus = jobExecution.exitStatus

        if (previousExitStatus.exitCode != ExitStatus.FAILED.exitCode) {
            return
        }

        def messages = []
        def seenThrowables = [] as Set
        jobExecution.allFailureExceptions.each {
            for (Throwable t = it; t != null; t = t.cause) {
                if (t in seenThrowables) {
                    return
                }
                seenThrowables << t
                messages << "${t.getClass().simpleName}: ${t.message}"
            }
        }

        jobExecution.exitStatus = new ExitStatus(
                previousExitStatus.exitCode, messages.join("\n"))
    }
}
