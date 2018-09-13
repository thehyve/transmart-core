package org.transmartproject.rest

import grails.converters.JSON
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.transmartproject.core.binding.BindingHelper
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.multidimquery.export.DataView
import org.transmartproject.db.job.AsyncJobCoreDb
import org.transmartproject.rest.dataExport.ExportAsyncJobService
import org.transmartproject.rest.dataExport.ExportService
import org.transmartproject.rest.marshallers.ContainerResponseWrapper
import org.transmartproject.rest.user.AuthContext
import org.transmartproject.core.multidimquery.export.ExportJobRepresentation

import java.util.stream.Collectors

import static org.transmartproject.rest.misc.RequestUtils.checkForUnsupportedParams

class ExportController {

    @Autowired
    ExportService restExportService
    @Autowired
    ExportAsyncJobService exportAsyncJobService
    @Autowired
    AuthContext authContext
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

        def instance = exportAsyncJobService.createNewJob(name, authContext.user)
        render wrapExportJob(instance) as JSON
    }

    /**
     * Run a data export job asynchronously:
     * <code>/v2/export/${jobId}/run</code>
     * Request body:
     * <code>
     * {
     *      constraint: <constraint json>
     *      elements: {
     *          dataType: "clinical" //supported data type
     *          format: "<TSV/SPSS/...>" //supported file format
     *          dataView: "<data view>" //optional
     *      }
     *      tableConfig: { //additional config, required for the data table export
     *          rowDimensions: [ "<list of dimension names>" ] //specifies the row dimensions of the data table
     *          columnDimensions: [ "<list of dimension names>" ] //specifies the column dimensions of the data table
     *          rowSort: [ "<list of sort specifications>" ] // Json list of sort specifications for the row dimensions
     *          columnSort: [ "<list of sort specifications>" ] // Json list of sort specifications for the column dimensions
     *      }
     * }
     * </code>
     * Creates a hypercube for each element from ${elements} that satisfies ${criteria}
     * and serialises it to specified $(fileFormat}.
     * Output stream is saved as .zip file in <code>tempFolderDirectory</code>, specified in configuration file.
     *
     * @param jobId - id of previously created job with status: 'Created'
     * @return The first time at which the <code>Trigger</code> to run the export will be fired
     *         by the scheduler
     */
    def run(@PathVariable('jobId') Long jobId) {
        checkForUnsupportedParams(params, ['jobId'])

        def exportJob = BindingHelper.read(request.inputStream, ExportJobRepresentation.class)

        if (exportJob.elements.any { it.dataView == DataView.DATA_TABLE }) {
            if (!exportJob.tableConfig) {
                throw new InvalidArgumentsException("No tableConfig provided.")
            }
        }

        def job = exportAsyncJobService.exportData(exportJob, jobId, authContext.user)
        render wrapExportJob(job) as JSON
    }

    /**
     * Cancels a job
     * <code>POST /v2/export/${jobId}/cancel
     * @param jobId
     */
    def cancel(@PathVariable('jobId') Long jobId) {
        checkForUnsupportedParams(params, ['jobId'])
        exportAsyncJobService.cancelJob(jobId, authContext.user)
        return true
    }

    /**
     * Deletes a job
     * <code>DELETE /v2/export/${jobId}
     * @param jobId
     */
    def delete(@PathVariable('jobId') Long jobId) {
        checkForUnsupportedParams(params, ['jobId'])
        exportAsyncJobService.deleteJob(jobId, authContext.user)
        return true
    }

    /**
     * Gets a job:
     * <code>GET /v2/export/${jobId}
     * @param jobId
     * @return the job json
     */
    def get(@PathVariable('jobId') Long jobId) {
        checkForUnsupportedParams(params, ['jobId'])
        def job = exportAsyncJobService.getJobById(jobId, authContext.user)
        render wrapExportJob(job) as JSON
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

        def job = exportAsyncJobService.getJobById(jobId, authContext.user)
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
     * @use get instead
     * @param jobId
     * @return current status of the job
     */
    @Deprecated
    def jobStatus(@PathVariable('jobId') Long jobId) {
        get(jobId)
    }

    /**
     * List dataExport jobs created by the user:
     * <code>/v2/export/jobs
     *
     * @return {@link AsyncJobCoreDb} instances
     */
    def listJobs() {
        checkForUnsupportedParams(params, [])
        def results = exportAsyncJobService.getJobList(authContext.user)
        render wrapExportJobs(results) as JSON
    }

    /**
     * Returns the supported data format: `clinical`.
     * The constraint parameter is for future use, to check if
     * data of a certain type is available that is supported by a data format.
     * <code>/v2/export/data_formats?constraint=${criteria}
     *
     * @param constraint to check if data for a data format is present
     * @return data formats
     */
    def dataFormats() {
        checkForUnsupportedParams(params, ['constraint'])

        respond dataFormats: ['clinical']
    }

    /**
     * List supported file formats:
     * <code>/v2/export/file_formats?dataView=<data view>
     *
     * @return File format types
     */
    def fileFormats(@RequestParam('dataView') String dataView) {
        checkForUnsupportedParams(params, ['dataView'])
        def view = DataView.from(dataView)
        if (view == DataView.NONE) {
            throw new InvalidArgumentsException("Unknown data view: ${dataView}")
        }
        def fileFormats = restExportService.getSupportedFormats(view)

        respond fileFormats: fileFormats
    }

    @CompileStatic
    private static Map<String, Object> convertToMap(AsyncJobCoreDb job) {
        [
                id           : job.id,
                jobName      : job.jobName,
                jobStatus    : job.jobStatus,
                jobStatusTime: job.jobStatusTime,
                userId       : job.userId,
                viewerUrl    : job.viewerURL,
                message      : job.results
        ] as Map<String, Object>
    }

    @CompileStatic
    private static ContainerResponseWrapper wrapExportJob(AsyncJobCoreDb source) {
        Map<String, Object> serializedJob = convertToMap(source)

        new ContainerResponseWrapper(
                key: 'exportJob',
                container: serializedJob,
                componentType: Map
        )
    }

    @CompileStatic
    private static ContainerResponseWrapper wrapExportJobs(List<AsyncJobCoreDb> sources) {
        List<Map<String, Object>> serializedJobs = sources.stream()
                .map({ AsyncJobCoreDb job -> convertToMap(job) })
                .collect(Collectors.toList())

        new ContainerResponseWrapper(
                key: 'exportJobs',
                container: serializedJobs,
                componentType: Map
        )
    }

}
