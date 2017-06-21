package smartr.tasks

import com.google.common.collect.ImmutableMap
import groovy.transform.ToString

@ToString(includePackage = false, includeNames = true)
final class TaskResult {
    final boolean successful
    final Exception exception
    final ImmutableMap<String, Object> artifacts

    TaskResult(Map<String, ? extends Object> args) {
        this.successful = args.successful
        this.exception = args.exception
        this.artifacts = args.artifacts
    }
}
