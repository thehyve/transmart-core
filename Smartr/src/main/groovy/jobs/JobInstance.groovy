package jobs

import tasks.Task

interface JobInstance {

    String getWorkflow()

    Task createTask(String name, Map<String, Object> arguments)
}
