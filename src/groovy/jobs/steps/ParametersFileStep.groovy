package jobs.steps

class ParametersFileStep implements Step{

    File temporaryDirectory
    Map<String, Object> params

    final String statusName = null

    @Override
    void execute() {
        File jobInfoFile = new File(temporaryDirectory, 'jobInfo.txt')

        jobInfoFile.withWriter { BufferedWriter it ->
            it.writeLine 'Parameters'
            params.each { key, value ->
                it.writeLine "\t$key -> $value"
            }
        }
    }
}
