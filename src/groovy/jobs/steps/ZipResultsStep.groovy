package jobs.steps


class ZipResultsStep implements Step {

    public static final String WORKING_DIRECTORY_NAME = 'workingDirectory'
    public static final String ZIP_FILE_NAME = 'zippedData.zip'
    final String statusName = 'Zipping results'

    def jobName
    def grailsApplication
    def zipService

    /**
     * The directOry where the job data is stored and from where the R scripts
     * run.
     *
     * The odd name ("folderDirectory") is an historical artifact.
     *
     * @return the jobs directory
     */
    private String getTempFolderDirectory() {
        String dir = grailsApplication.config.RModules.tempFolderDirectory
        if (dir && !dir.endsWith(File.separator)) {
            dir += File.separator
        }
        dir
    }

    @Override
    void execute() {
        String analysisDirectory = tempFolderDirectory + jobName + File.separator
        String tempDirectory = analysisDirectory + WORKING_DIRECTORY_NAME + File.separator
        String zipLocation = analysisDirectory + ZIP_FILE_NAME
        zipService.zipFolder(tempDirectory, zipLocation)
    }
}
