package org.transmartproject.rest

import grails.converters.JSON
import org.grails.web.converters.exceptions.ConverterException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.user.User
import org.transmartproject.rest.dataExport.RestExportService
import org.transmartproject.rest.misc.CurrentUser
import com.recomdata.transmart.domain.i2b2.AsyncJob

class RestExportController {
    
    @Autowired
    RestExportService restExportService
    @Autowired
    CurrentUser currentUser
    @Autowired
    UsersResource usersResource

    static def globalParams = [
            "controller",
            "action",
            "format",
            "apiVersion"
    ]

    /**
     * Create a new asynchronous dataExport job:
     * <code>/v2/export/job?jobName=${jobName}</code>
     *
     * If the ${jobName} is not specified, a default name will be created.
     * ${jobName} = username-jobtype-ID
     *
     * @param jobName - optional
     * @return {@link AsyncJob} instance
     */
    def createJob(@RequestParam('jobName') String jobName) {
        checkParams(params, ['jobName'])

        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        checkJobNameUnique(jobName)

        def instance = restExportService.createExportJob(user, jobName)
        respond instance
    }

    /**
     * Run a data export job asynchronously:
     * <code>/v2/export/${jobName}/run?setType=${setType}&ids=${ids}&elements=${elements}</code>
     *
     * Creates a hypercube for each element from ${elements} with PatientSetsConstraint for given ${ids}
     * (see {@link org.transmartproject.db.dataquery.clinical.patientconstraints.PatientSetsConstraint})
     * and serialises it to specified $(fileFormat}.
     * Output stream is saved as .zip file in <code>tempFolderDirectory</code>, specified in configuration file.
     *
     * @param setType - 'patient' or 'observation' set
     * @param jobName - name of previously created job with 'Started' status
     * @param ids - list of sets ids
     * @param elements - list of pairs: {dataType:${dataType}, format:$(fileFormat}, JSON format
     * @return The first time at which the <code>Trigger</code> to run the export will be fired
     *         by the scheduler
     */
    def run(@RequestParam(value='setType') String setType,
            @PathVariable('jobName') String jobName) {

        checkParams(params, ['setType', 'jobName', 'ids', 'elements'])
        List<Long> ids = parseIds(params)
        List<Map> elements = parseElements(params)
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        checkSetTypeSupported(setType)
        checkRightsToExport(ids, user, setType)
        checkJobAccess(jobName, user)

        def scheduledTime = restExportService.exportData(ids, setType, elements, user, jobName)

        def result = [ jobName : jobName, scheduledTime : scheduledTime ]
        render result as JSON
    }

    /**
     * Download saved .zip file with exported data:
     * <code>/v2/export/${jobName}/download
     *
     * @param jobName
     * @return zipOutputStream
     */
    def download(@PathVariable('jobName') String jobName) {

        checkParams(params, ['jobName'])
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        checkJobAccess(jobName, user)

        def InputStream inputStream = restExportService.downloadFile(jobName)

        def fileName = jobName + ".zip"
        response.setContentType 'application/zip'
        response.setHeader "Content-disposition", "attachment;filename=${fileName}"
        response.outputStream << inputStream
        response.outputStream.flush()
        inputStream.close()
        return true
    }

    /**
     * Check a status of specified job:
     * <code>/v2/export/${jobName}/status
     *
     * @param jobName
     * @return current status of the job
     */
   def jobStatus(@PathVariable('jobName') String jobName) {
       checkParams(params, ['jobName'])
       def jobStatus = restExportService.jobStatus(jobName)
       if (!jobStatus) {
           throw new InvalidArgumentsException("Job with a name '$jobName' does not exist.")
       }
       def results = [jobStatus: jobStatus]
       render results as JSON
   }

    /**
     * List dataExport jobs created by the user:
     * <code>/v2/export/jobs
     *
     * @return {@link AsyncJob} instances
     */
    def listJobs() {
        checkParams(params, [])
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        def results = restExportService.listJobs(user)
        render results as JSON
    }

    /**
     * Get available types of the data for specified set id,
     * `clinical` for clinical data and supported high dimensional data types.
     * <code>/v2/export/data_formats/${ids}?setType=${setType}
     *
     * @param setType
     * @param ids
     * @return data formats
     */
    def dataFormats(@RequestParam('setType') String setType) {

        checkParams(params, ['setType', 'ids'])
        List<Long> ids = parseIds(params)
        checkSetTypeSupported(setType)
        User user = (User) usersResource.getUserFromUsername(currentUser.username)

        def formats = restExportService.getDataFormats(setType, ids, user)
        def results = [dataFormats: formats]
        render results as JSON
    }

    /**
     * List supported file formats:
     * <code>/v2/export/supported_file_formats
     *
     * @return File format types
     */
    def fileFormats() {
        checkParams(params, [])
        def fileFormats = restExportService.supportedFileFormats
        def results = [supportedFileFormats: fileFormats]
        render results as JSON
    }



    private void checkRightsToExport(List<Long> resultSetIds, User user, String setType) {
        try {
            restExportService.isUserAllowedToExport(resultSetIds, user, setType)
        } catch (UnsupportedOperationException e) {
            throw new AccessDeniedException("User ${user.username} has no EXPORT permission" +
                    " on one of the result sets: ${resultSetIds.join(', ')}")
        }
    }
    
    private checkJobAccess(String jobName, User user) {
        String jobUsername = restExportService.jobUser(jobName)
        if (!jobUsername) throw new InvalidArgumentsException("Job with a name '$jobName' does not exists.")

        if (user.isAdmin()) return
        else if (jobUsername != user.username) {
            log.warn("Denying access to job $jobName because the " +
                    "corresponding username ($jobUsername) does not match " +
                    "that of the current user")
            throw new AccessDeniedException("Job $jobName was not started by " +
                    "this user")
        }
    }

    private void checkJobNameUnique(String jobName) {
        String name = jobName?.trim()
        if(name && !restExportService.isJobNameUnique(name)) {
            throw new InvalidArgumentsException("Given job name: '$name' already exists.")
        }
    }

    private static void checkParams(Map params, Collection<String> acceptedParams) {
        acceptedParams.addAll(globalParams)
        params.keySet().each { param ->
            if (!acceptedParams.contains(param)) {
                throw new InvalidArgumentsException("Parameter not supported: $param.")
            }
        }
    }

    private void checkSetTypeSupported(String setType) {
        if (!(setType?.trim() in restExportService.supportedSetTypes)) {
            throw new InvalidArgumentsException("Type not supported: $setType.")
        }
    }

    private static List<Long> parseIds(params) {
        if (!params.ids){
            throw new InvalidArgumentsException('Empty ids parameter.')
        }
        try {
            return params.getList('ids').collect { it as Long }
        } catch (NumberFormatException e) {
            throw new InvalidArgumentsException('Id parameter should be a number.')
        }
    }

    private static List<Map> parseElements(params) {
        if (!params.elements) {
            throw new InvalidArgumentsException('Empty elements map.')
        }
        try {
            return JSON.parse(params.elements)
        } catch (ConverterException c) {
            throw new InvalidArgumentsException("Cannot parse parameter: $params.elements")
        }
    }
}
