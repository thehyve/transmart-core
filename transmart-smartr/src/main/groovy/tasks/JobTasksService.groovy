package heim.tasks

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import groovy.transform.ToString
import groovy.util.logging.Log4j
import heim.SmartRExecutorService
import heim.session.SessionContext
import heim.session.SessionService
import heim.session.SmartRSessionScope
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.core.exceptions.NoSuchResourceException

import java.lang.reflect.UndeclaredThrowableException
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by glopes on 09-10-2015.
 */
@Log4j
@Component
@SmartRSessionScope
class JobTasksService implements DisposableBean {

    @Autowired
    private SmartRExecutorService smartRExecutorService

    @Autowired
    private SessionService sessionService

    @Value("#{sessionId}")
    private UUID sessionId

    private volatile boolean shuttingDown

    @ToString(includePackage = false, includes = ['state', 'taskResult'], includeNames = true)
    static final class TaskAndState {
        final Task task
        final TaskState state
        final TaskResult taskResult

        TaskAndState(Map<String, ? extends Object> args) {
            this.task = args.task
            this.state = args.state
            this.taskResult = args.taskResult
        }
    }

    private final Map<UUID, TaskAndState> tasks = new ConcurrentHashMap<>()
    private final Map<UUID, ListenableFuture<TaskResult>> futures =
            new ConcurrentHashMap<>() // future removed when task finishes/fails
    private final Map<UUID, SettableFuture<TaskResult>> publicFutures =
            new ConcurrentHashMap<>()

    boolean hasActiveTasks() {
        !futures.isEmpty()
    }

    void submitTask(Task task) {
        if (shuttingDown) {
            throw new IllegalStateException('Shutting down already')
        }

        def taskAndState = new TaskAndState(
                task: task,
                state: TaskState.QUEUED)

        tasks[task.uuid] = taskAndState

        log.debug "Task $task will be submitted now"
        ListenableFuture<TaskResult> future = smartRExecutorService.submit(
                new Callable() {
                    @Override
                    Object call() throws Exception {
                        log.debug "Task $task entered running state"
                        tasks[task.uuid] = new TaskAndState(
                                task: task,
                                state: TaskState.RUNNING,
                        )

                        sessionService.doWithSession(sessionId) {
                            task.call()
                        }
                    }
                })

        futures[task.uuid] = future

        def publicFuture = new SettableFuture<TaskResult>()
        publicFutures[task.uuid] = publicFuture

        Futures.addCallback(future, new FutureCallback<TaskResult>() {
            void onSuccess(TaskResult taskResult1) {
                assert taskResult1 != null :
                        'Task must return TaskResult or throw'
                log.debug("Task $task terminated without throwing. " +
                        "Successful? $taskResult1.successful")
                tasks[task.uuid] = new TaskAndState(
                        task: task,
                        state: taskResult1.successful ?
                                TaskState.FINISHED :
                                TaskState.FAILED,
                        taskResult: taskResult1)
                common(tasks[task.uuid])
            }
            void onFailure(Throwable thrown) {
                if (thrown instanceof UndeclaredThrowableException) {
                    thrown = thrown.undeclaredThrowable
                }
                if (thrown instanceof CancellationException) {
                    log.debug("Task $task was cancelled")
                } else {
                    log.debug "Task $task terminated by throwing", thrown
                }

                tasks[task.uuid] = new TaskAndState(
                        task: task,
                        state: TaskState.FAILED,
                        taskResult: new TaskResult(
                                successful: false,
                                exception: thrown,
                        ))
                common(tasks[task.uuid])
            }

            private void common(TaskAndState result) {
                futures.remove(task.uuid)
                try {
                    task.close()
                } catch (Exception e) {
                    log.error("Failed calling close() on task $task", e)
                }
                publicFuture.set(result.taskResult)
                sessionService.touchSession(sessionId) // should not throw
                log.info "Task $task finished. Final result: $result"
            }
        }) // run on the same thread
    }

    ListenableFuture<TaskResult> getTaskResultFuture(UUID taskId) {
        publicFutures.get(taskId)
    }

    TaskState getTaskState(UUID uuid) {
        tasks.get(uuid)?.state
    }

    TaskResult getTaskResult(UUID uuid) {
        tasks.get(uuid)?.taskResult
    }

    @Override
    void destroy() throws Exception {
        // try to interrupt/cancel all the running tasks
        shuttingDown = true

        futures.values().each {
            it.cancel(true)
        }
    }
}
