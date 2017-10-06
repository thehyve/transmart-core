package org.transmartproject.rest

import grails.converters.JSON
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.multidimquery.MultiDimConstraint
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.job.AsyncJobCoreDb
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.ConstraintFactory
import org.transmartproject.db.user.User
import org.transmartproject.rest.dataExport.ExportAsyncJobService
import org.transmartproject.rest.dataExport.ExportService
import org.transmartproject.rest.marshallers.ContainerResponseWrapper
import org.transmartproject.rest.misc.CurrentUser
import static org.transmartproject.rest.misc.RequestUtils.checkForUnsupportedParams

class ExportController {

    @Autowired
    ExportService restExportService
    @Autowired
    ExportAsyncJobService exportAsyncJobService
    @Autowired
    CurrentUser currentUser
    @Autowired
    UsersResource usersResource
    @Autowired
    MultiDimensionalDataResource multiDimService

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
        checkForUnsupportedParams(params, ['name'])

        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        checkJobNameUnique(name, user)

        def instance = exportAsyncJobService.createNewJob(user, name)
        respond wrapExportJob(instance)
    }

    /**
     * Run a data export job asynchronously:
     * <code>/v2/export/${jobId}/run</code>
     * Request body:
     * <code>
     * {
     *      id: [id1,....idx], //list of query result ids. Could be "observation sets" or "patient sets"
     *      criteria: <criteria json>
     *      elements: {
     *          dataType: "<clinical/mrna/...>" //supported data type
     *          format: "<TSV/SPSS/...>" //supported file format
     *          tabular: <true/false> //optional, false by default.
     *          //When tabular = true => represent hypercube as table with a subject per row and variable per column
     *      }
     * }
     * </code>
     * Creates a hypercube for each element from ${elements} that satisfies ${id} or ${criteria} (one of two could be supplied)
     * and serialises it to specified $(fileFormat}.
     * Output stream is saved as .zip file in <code>tempFolderDirectory</code>, specified in configuration file.
     *
     * @param jobId - id of previously created job with status: 'Created'
     * @return The first time at which the <code>Trigger</code> to run the export will be fired
     *         by the scheduler
     */
    def run(@PathVariable('jobId') Long jobId) {
        checkForUnsupportedParams(params, ['jobId'])
        def requestBody = request.JSON as Map
        def notSupportedFields = requestBody.keySet() - ['id', 'elements', 'constraint']
        if (notSupportedFields) {
            throw new InvalidArgumentsException("Following fields are not supported ${notSupportedFields}.")
        }
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        checkJobAccess(jobId, user)
        if (!requestBody.elements) {
            throw new InvalidArgumentsException('Empty elements map.')
        }

        Constraint constraint = getConstraintFromJson(requestBody, user)

        def job = exportAsyncJobService.exportData(constraint, requestBody.elements, user, jobId)

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

        checkForUnsupportedParams(params, ['jobId'])
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        checkJobAccess(jobId, user)

        def job = exportAsyncJobService.getJobById(jobId)
        InputStream inputStream = restExportService.downloadFile(job)

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
       checkForUnsupportedParams(params, ['jobId'])
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
        checkForUnsupportedParams(params, [])
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        def results = exportAsyncJobService.getJobList(user)
        respond wrapExportJobs(results)
    }

    /**
     * Get available types of the data for specified set id,
     * `clinical` for clinical data and supported high dimensional data types.
     * <code>/v2/export/data_formats?id=${id1}&...&id=${idx}
     *
     * @param id - list of sets ids, multiple parameter instances format
     * @return data formats
     */
    def dataFormats() {

        checkForUnsupportedParams(params, ['id'])
        User user = (User) usersResource.getUserFromUsername(currentUser.username)
        MultiDimConstraint constraint = multiDimService.createQueryResultsDisjunctionConstraint(parseId(params), user)
        def formats = ['clinical'] + multiDimService.retrieveHighDimDataTypes(constraint, user)
        def results = [
                dataFormats: formats
        ]
        render results as JSON
    }

    /**
     * List supported file formats:
     * <code>/v2/export/file_formats
     *
     * @return File format types
     */
    def fileFormats() {
        checkForUnsupportedParams(params, [])
        def fileFormats = restExportService.supportedFileFormats
        def results = [fileFormats: fileFormats]
        render results as JSON
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

    private Constraint getConstraintFromJson(json, User user) {
        if (!(json.containsKey('id') ^ json.containsKey('constraint'))) {
            throw new InvalidArgumentsException("Whether id or constraint parameters can be supplied.")
        } else if (json.containsKey('id')) {
            return multiDimService.createQueryResultsDisjunctionConstraint(json.id instanceof Number ? [json.id] : json.id, user)
        } else if (json.containsKey('constraint')) {
            return ConstraintFactory.create(json.constraint)
        }
    }
}
