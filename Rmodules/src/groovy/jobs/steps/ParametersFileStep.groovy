package jobs.steps

import groovy.json.JsonOutput
import jobs.UserParameters

class ParametersFileStep implements Step{

    File temporaryDirectory
    UserParameters params

    final String statusName = 'Writing parameters'

    @Override
    void execute() {
        File jobInfoFile = new File(temporaryDirectory, 'jobInfo.txt')

        jobInfoFile.withWriter { BufferedWriter it ->
            it.writeLine 'Parameters'
            params.each { key, value ->
                it.writeLine "\t$key -> $value"
            }
        }
        File paramsFile = new File(temporaryDirectory, 'request.json')
        paramsFile << JsonOutput.prettyPrint(params.toJSON())
    }
}
