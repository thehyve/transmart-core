package smartr

import grails.validation.Validateable
import session.SessionService
import tasks.TaskResult
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

class StatusCommand implements Validateable {
    UUID sessionId
    UUID executionId
    boolean waitForCompletion

    static constraints = {
        sessionId   nullable: false
        executionId nullable: false
    }
}

class RunCommand implements Validateable {
    UUID sessionId
    Map arguments = [:]
    String taskType

    static constraints = {
        sessionId nullable: false
        taskType  blank: false
    }
}

class DownloadCommand implements Validateable {
    UUID sessionId
    UUID executionId
    String filename

    static constraints = {
        sessionId   nullable: false
        executionId nullable: false
        filename    blank: false
    }
}
