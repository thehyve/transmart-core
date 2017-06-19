package heim.tasks

import java.util.concurrent.Callable

/**
 * Created by glopes on 09-10-2015.
 */
interface Task extends Callable<TaskResult>, AutoCloseable {

    UUID getUuid()

}
