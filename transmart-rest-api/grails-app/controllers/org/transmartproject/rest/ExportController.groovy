package org.transmartproject.rest

import grails.converters.JSON
import org.grails.web.converters.exceptions.ConverterException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.job.AsyncJobCoreDb
import org.transmartproject.db.user.User
import org.transmartproject.rest.dataExport.ExportAsyncJobService
import org.transmartproject.rest.dataExport.ExportService
import org.transmartproject.rest.marshallers.ContainerResponseWrapper
import org.transmartproject.rest.misc.CurrentUser

class ExportController {

    @Autowired
    ExportService restExportService
    @Autowired
    ExportAsyncJobService exportAsyncJobService
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
     * <code>/v2/export/job?name=${name}</code>
     *
     * If the ${name} is not specified, job id will be set as a default name.
     *
     * @param name - optional
     * @return {@link AsyncJobCoreDb} instance
     */
    def createJob(@RequestParam('name') String name) {
        checkParams(params, ['name'])

        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        checkJobNameUnique(name, user)

        def instance = exportAsyncJobService.createNewJob(user, name)
        respond wrapExportJob(instance)
    }

    /**
     * Run a data export job asynchronously:
     * <code>/v2/export/${jobId}/run?typeOfSet=${typeOfSet}&id=${id1}&...&id=${idx}&elements=${elements}</code>
     *
     * Creates a hypercube for each element from ${elements} with PatientSetsConstraint for given ${ids}
     * (see {@link org.transmartproject.db.dataquery.clinical.patientconstraints.PatientSetsConstraint})
     * and serialises it to specified $(fileFormat}.
     * Output stream is saved as .zip file in <code>tempFolderDirectory</code>, specified in configuration file.
     *
     * @param typeOfSet - 'patient' or 'observation' set
     * @param jobId - id of previously created job with status: 'Created'
     * @param id - list of sets ids, multiple parameter instances format
     * @param elements - list of pairs: {dataType:${dataType}, format:$(fileFormat}, JSON format
     * @return The first time at which the <code>Trigger</code> to run the export will be fired
     *         by the scheduler
     */
    def run(@RequestParam('typeOfSet') String typeOfSet,
            @PathVariable('jobId') Long jobId) {

        checkParams(params, ['typeOfSet', 'jobId', 'id', 'elements'])
        List<Long> id = parseId(params)
        List<Map> elements = parseElements(params)
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        checkTypeOfSetSupported(typeOfSet)
        checkRightsToExport(id, user, typeOfSet)
        checkJobAccess(jobId, user)

        def job = exportAsyncJobService.exportData(id, typeOfSet, elements, user, jobId)

        respond wrapExportJob(job)
    }

    /**
     * Download saved .zip file with exported data:
     * <code>/v2/export/${jobId}/download
     *
     * @param jobId
     * @return zipOutputStream
     */
    def download(@PathVariable('jobId') Long jobId) {

        checkParams(params, ['jobId'])
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        checkJobAccess(jobId, user)

        def job = exportAsyncJobService.getJobById(jobId)
        def InputStream inputStream = restExportService.downloadFile(job)

        response.setContentType 'application/zip'
        response.setHeader "Content-disposition", "attachment;"//filename=${inputStream.ass}"
        response.outputStream << inputStream
        response.outputStream.flush()
        inputStream.close()
        return true
    }

    /**
     * Check a status of specified job:
     * <code>/v2/export/${jobId}/status
     *
     * @param jobId
     * @return current status of the job
     */
   def jobStatus(@PathVariable('jobId') Long jobId) {
       checkParams(params, ['jobId'])
       def job = exportAsyncJobService.getJobById(jobId)
       if (!job) {
           throw new InvalidArgumentsException("Job with id '$jobId' does not exist.")
       }
       respond wrapExportJob(job)
   }

    /**
     * List dataExport jobs created by the user:
     * <code>/v2/export/jobs
     *
     * @return {@link AsyncJobCoreDb} instances
     */
    def listJobs() {
        checkParams(params, [])
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        def results = exportAsyncJobService.getJobList(user)
        respond wrapExportJobs(results)
    }

    /**
     * Get available types of the data for specified set id,
     * `clinical` for clinical data and supported high dimensional data types.
     * <code>/v2/export/data_formats?id=${id1}&...&id=${idx}&typeOfSet=${typeOfSet}
     *
     * @param typeOfSet
     * @param id - list of sets ids, multiple parameter instances format
     * @return data formats
     */
    def dataFormats(@RequestParam('typeOfSet') String typeOfSet) {

        checkParams(params, ['typeOfSet', 'id'])
        List<Long> id = parseId(params)
        checkTypeOfSetSupported(typeOfSet)
        User user = (User) usersResource.getUserFromUsername(currentUser.username)

        def formats = restExportService.getDataFormats(typeOfSet, id, user)
        def results = [dataFormats: formats]
        render results as JSON
    }

    /**
     * List supported file formats:
     * <code>/v2/export/file_formats
     *
     * @return File format types
     */
    def fileFormats() {
        checkParams(params, [])
        def fileFormats = restExportService.supportedFileFormats
        def results = [fileFormats: fileFormats]
        render results as JSON
    }



    private void checkRightsToExport(List<Long> resultSetIds, User user, String typeOfSet) {
        try {
            restExportService.isUserAllowedToExport(resultSetIds, user, typeOfSet)
        } catch (UnsupportedOperationException e) {
            throw new AccessDeniedException("User ${user.username} has no EXPORT permission" +
                    " on one of the result sets: ${resultSetIds.join(', ')}")
        }
    }
    
    private checkJobAccess(Long jobId, User user) {
        String jobUsername = exportAsyncJobService.getJobUser(jobId)
        if (!jobUsername) throw new InvalidArgumentsException("Job with id '$jobId' does not exists.")

        if (user.isAdmin()) return
        else if (jobUsername != user.username) {
            log.warn("Denying access to job $jobId because the " +
                    "corresponding username ($jobUsername) does not match " +
                    "that of the current user")
            throw new AccessDeniedException("Job $jobId was not created by " +
                    "this user")
        }
    }

    private void checkJobNameUnique(String jobName, User user) {
        String name = jobName?.trim()
        if(name && !exportAsyncJobService.isJobNameUniqueForUser(name, user)) {
            throw new InvalidArgumentsException("Given job name: '$name' already exists for user '$user.")
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

    private void checkTypeOfSetSupported(String typeOfSet) {
        if (!(typeOfSet?.trim() in restExportService.supportedTypesOfSet)) {
            throw new InvalidArgumentsException("Type of set not supported: $typeOfSet.")
        }
    }

    private static List<Long> parseId(params) {
        if (!params.id){
            throw new InvalidArgumentsException('Empty id parameter.')
        }
        try {
            return params.getList('id').collect { it as Long }
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

    private static Map<String, Object> convertToMap(AsyncJobCoreDb job) {
        [
                id           : job.id,
                jobName      : job.jobName,
                jobStatus    : job.jobStatus,
                jobStatusTime: job.jobStatusTime,
                userId       : job.userId,
                viewerUrl    : job.viewerURL
        ]
    }

    private static ContainerResponseWrapper wrapExportJob(AsyncJobCoreDb source) {
        Map<String, Object> serializedJob = convertToMap(source)

        new ContainerResponseWrapper(
                key: 'exportJob',
                container: serializedJob,
                componentType: Map,
        )
    }

    private static ContainerResponseWrapper wrapExportJobs(List<AsyncJobCoreDb> sources) {
        List<Map<String, Object>> serializedJobs = sources.collect {
            convertToMap(it)
        }

        new ContainerResponseWrapper(
                key: 'exportJobs',
                container: serializedJobs,
                componentType: Map,
        )
    }
}
