package jobs.steps

class WriteFileStep implements Step {

    File temporaryDirectory
    String fileName
    String fileContent

    @Override
    String getStatusName() {
        if (sufficientInformationProvided) {
            "Writing ${fileName} file"
        }
    }

    @Override
    void execute() {
        if (sufficientInformationProvided) {
            new File(temporaryDirectory, fileName).text = fileContent
        }
    }

    private boolean isSufficientInformationProvided() {
        temporaryDirectory && fileName && fileContent
    }

}
