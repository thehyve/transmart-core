package heim.tasks

import org.springframework.core.Ordered

/**
 * Created by glopes on 09-10-2015.
 */
interface TaskFactory extends Ordered {

    boolean handles(String taskName,
                    Map<String, Object> argument)

    Task createTask(String name,
                    Map<String, Object> arguments)
}
