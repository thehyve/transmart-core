package smartr.tasks

import org.springframework.core.Ordered

interface TaskFactory extends Ordered {

    boolean handles(String taskName,
                    Map<String, Object> argument)

    Task createTask(String name,
                    Map<String, Object> arguments)
}
