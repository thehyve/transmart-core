package heim.jobs

import heim.tasks.Task

/**
 * Created by glopes on 13-10-2015.
 */
interface JobInstance {

    String getWorkflow()

    Task createTask(String name, Map<String, Object> arguments)
}
