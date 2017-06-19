package heim.tasks

import heim.tasks.AbstractTask
import heim.tasks.Task
import heim.tasks.TaskFactory
import heim.tasks.TaskResult
import org.springframework.core.Ordered

import java.util.concurrent.atomic.AtomicBoolean

/**
 * This class is here for technical reasons (test classpath is not available
 * when setting up the Spring context).
 */
class TestTaskFactory implements TaskFactory {

    public final static String TEST_TASK_NAME = 'test'

    final int order = Ordered.HIGHEST_PRECEDENCE

    @Override
    boolean handles(String taskName, Map<String, Object> argument) {
        taskName == TEST_TASK_NAME
    }

    @Override
    Task createTask(String name, Map<String, Object> arguments) {
        assert arguments['closure'] instanceof Closure
        new AbstractTask() {
            AtomicBoolean isClosed = arguments.closed
            final Object monitor = arguments.monitor

            @Override
            void close() throws Exception {
                synchronized (monitor) {
                    isClosed?.set(true)
                    monitor.notify()
                }
            }

            @Override
            TaskResult call() throws Exception {
                arguments['closure'].call()
            }
        }
    }
}
