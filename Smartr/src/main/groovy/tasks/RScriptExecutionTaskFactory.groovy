package tasks

import misc.SmartRRuntimeConstants
import jobs.JobInstance
import rserve.GenericJavaObjectAsJsonRFunctionArg
import rserve.RFunctionArg
import rserve.RScriptsSynchronizer
import rserve.RServeSession
import session.SessionFiles
import session.SmartRSessionScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.transmartproject.core.exceptions.InvalidArgumentsException

@Component
@SmartRSessionScope
class RScriptExecutionTaskFactory implements TaskFactory {

    @Autowired
    private RServeSession rServeSession

    @Autowired
    private JobInstance jobInstance

    @Autowired
    private RScriptsSynchronizer rScriptsSynchronizer

    @Autowired
    private SessionFiles sessionFiles

    @Autowired
    private SmartRRuntimeConstants constants

    @Value('#{sessionId}')
    private UUID sessionId

    final int order = Ordered.LOWEST_PRECEDENCE

    @Override
    boolean handles(String taskName, Map<String, Object> argument) {
        true // fallback factory, since it has the lowest precedence
    }

    private File calculateRemoteScriptPath(String taskType) {
        File dir = constants.remoteScriptDirectoryDir
        assert dir != null
        File workflowDir = new File(dir, jobInstance.workflow)
        new File(workflowDir, taskType + '.R')
    }

    @Override
    Task createTask(String taskName, Map<String, Object> arguments) {
        if (!rScriptsSynchronizer.wasCopySuccessful()) {
            throw new IllegalStateException(
                    "Cannot continue because R scripts were not successfully copied")
        }
        try {
            File fileToLoad = calculateRemoteScriptPath(taskName)

            new RScriptExecutionTask(
                    sessionFiles:  sessionFiles,
                    sessionId:     sessionId,
                    rServeSession: rServeSession,
                    fileToLoad:    fileToLoad,
                    arguments:     convertArguments(arguments),
                    constants:     constants,
            )
        } catch (IOException ioe) {
            throw new InvalidArgumentsException("Bad script '$taskName' for " +
                    "workflow '${jobInstance.workflow}'", ioe)
        }
    }

    List<RFunctionArg> convertArguments(Map<String, Object> arguments) {
        arguments.collect { k, v ->
            new GenericJavaObjectAsJsonRFunctionArg(
                    name: k,
                    object: v)
        }
    }
}
