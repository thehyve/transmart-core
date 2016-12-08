package com.recomdata.transmart.rmodules

import org.transmartproject.core.exceptions.InvalidRequestException

import java.util.regex.Matcher
import java.util.regex.Pattern

class AnalysisFilesController {

    public static final String ROLE_ADMIN = 'ROLE_ADMIN'

    def springSecurityService

    def RModulesOutputRenderService

    def download() {
        String jobName = params.analysisName

        if (!checkPermissions(jobName)) {
            render status: 403
            return
        }

        File analysisDirectory = new File(jobsDirectory, jobName)
        if (analysisDirectory.parentFile != jobsDirectory) {
            // just some sanity checking... should always happen
            log.error "Unexpected analysis directory: $analysisDirectory"
            render status: 404
            return
        }
        if (!analysisDirectory.exists()) {
            log.warn "Could not find directory for job " +
                    "$jobName: $analysisDirectory"
            render status: 404
            return
        }
        if (!analysisDirectory.isDirectory()) {
            log.error "Analysis directory is surprisingly " +
                    "not a directory: $analysisDirectory"
            render status: 404
            return
        }

        // Only expose files under the analysis directory
        File targetFile = new File(analysisDirectory, params.path)
        //canonical path does not end with separator
        if (!targetFile.canonicalPath
                .startsWith(analysisDirectory.canonicalPath + File.separator)) {

            log.warn "Request for $targetFile, but it's not " +
                    "under $analysisDirectory"
            render status: 404
            return
        }

        if (!targetFile.isFile()) {
            log.warn "Request for $targetFile, but such file does not exist"
            render status: 404
            return
        }

        render(file: targetFile, fileName: targetFile.name, contentType: servletContext.getMimeType(targetFile.name
                .toLowerCase()))
    }

    private boolean isAdmin() {
        springSecurityService.principal.authorities.any {
            it.authority == ROLE_ADMIN
        }
    }

    private File getJobsDirectory() {
        new File(RModulesOutputRenderService.tempFolderDirectory)
    }

    private boolean checkPermissions(String jobName) {
        String userName = extractUserFromJobName(jobName)

        def loggedInUser = springSecurityService.principal?.username
        if (!loggedInUser) {
            log.error 'Could not determine current logged in user\'s name'
            return false
        }

        if (userName == loggedInUser || admin) {
            return true
        }

        log.warn "User $loggedInUser has no access for job $jobName; refusing " +
                "request for job $jobName"
        false
    }

    private String extractUserFromJobName(String jobName) {
        Pattern pattern = ~/(.+)-[a-zA-Z]+-\d+/
        Matcher matcher = pattern.matcher(jobName)

        if (!matcher.matches()) {
            //should never happen due to url mapping
            throw new InvalidRequestException('Invalid job name')
        }

        matcher.group(1)
    }

}
