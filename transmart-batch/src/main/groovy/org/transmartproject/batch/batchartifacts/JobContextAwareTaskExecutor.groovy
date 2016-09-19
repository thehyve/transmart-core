package org.transmartproject.batch.batchartifacts

import org.springframework.batch.core.scope.context.JobSynchronizationManager
import org.springframework.core.task.SimpleAsyncTaskExecutor

/**
 * ThreadExecutor impl that makes sure the new thread's JobContext is registered
 * and bound to the original JobExecution object.
 */
class JobContextAwareTaskExecutor extends SimpleAsyncTaskExecutor {

    @Override
    protected void doExecute(Runnable task) {
        def jobExecution = JobSynchronizationManager.context.jobExecution
        super.doExecute(new Runnable() {
            @Override
            void run() {
                JobSynchronizationManager.register(jobExecution)

                try {
                    task.run()
                } finally {
                    JobSynchronizationManager.close()
                }
            }
        })
    }
}
