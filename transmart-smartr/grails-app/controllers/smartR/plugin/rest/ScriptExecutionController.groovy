package smartR.plugin.rest

import grails.validation.Validateable
import heim.session.SessionService
import heim.tasks.TaskResult
import org.transmartproject.core.exceptions.InvalidArgumentsException

class ScriptExecutionController {

    static scope = 'request'

    SessionService sessionService
    def sendFileService

    static allowedMethods = [
            init  : 'POST',
            run   : 'POST',
            status: 'GET',
            downloadFile: 'GET',
    ]

    def run(RunCommand runCommand) {
        throwIfInvalid runCommand

        UUID executionId =
                sessionService.createTask(
                        runCommand.arguments,
                        runCommand.sessionId,
                        runCommand.taskType,)

        render(contentType: 'text/json') {
            [executionId: executionId.toString()]
        }
    }

    def status(StatusCommand statusCommand) {
        throwIfInvalid statusCommand

        Map status = sessionService.getTaskData(
                statusCommand.sessionId,
                statusCommand.executionId,
                statusCommand.waitForCompletion)

        TaskResult res = status.result
        def resultValue = null
        if (res != null) {
            String exceptionMessage
            if (res.exception) {
                exceptionMessage = res.exception.getClass().toString() +
                        ': ' + res.exception.message
            }
            resultValue = [
                    successful: res.successful,
                    exception: exceptionMessage,
                    artifacts: res.artifacts,
            ]
        }

        render(contentType: 'text/json') {
            [
                    state : status.state.toString(),
                    result: resultValue
            ]
        }
    }

    def downloadFile(DownloadCommand downloadCommand) {
        throwIfInvalid downloadCommand

        File selectedFile = sessionService.getFile(
                downloadCommand.sessionId,
                downloadCommand.executionId,
                downloadCommand.filename)

        sendFileService.sendFile servletContext, request, response, selectedFile
    }

    private void throwIfInvalid(command) {
        if (command.hasErrors()) {
            List errorStrings = command.errors.allErrors.collect {
                g.message(error:it, encodeAs: 'raw')
            }
            throw new InvalidArgumentsException("Invalid input: $errorStrings")
        }
    }
}

@Validateable
class StatusCommand {
    UUID sessionId
    UUID executionId
    boolean waitForCompletion

    static constraints = {
        sessionId   nullable: false
        executionId nullable: false
    }
}

@Validateable
class RunCommand {
    UUID sessionId
    Map arguments = [:]
    String taskType

    static constraints = {
        sessionId nullable: false
        taskType  blank: false
    }
}

@Validateable
class DownloadCommand {
    UUID sessionId
    UUID executionId
    String filename

    static constraints = {
        sessionId   nullable: false
        executionId nullable: false
        filename    blank: false
    }
}
