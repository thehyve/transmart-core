package org.transmartproject.rest.dataExport

import grails.core.GrailsApplication
import grails.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.LegacyStudyException
import org.transmartproject.db.job.AsyncJobCoreDb
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.rest.MultidimensionalDataService
import org.transmartproject.rest.serialization.Format

import java.util.zip.ZipOutputStream

import static org.transmartproject.rest.serialization.Format.*

@Transactional
@Component("restExportService")
class ExportService {

    @Autowired
    MultidimensionalDataService multidimensionalDataService

    @Autowired
    GrailsApplication grailsApplication

    static supportedFileFormats = EnumSet.of(TSV, SPSS)

    def downloadFile(AsyncJobCoreDb job) {
        if(job.jobStatus != JobStatus.COMPLETED.value) {
            throw new InvalidRequestException("Job with a name is not completed. Current status: '$job.jobStatus'")
        }

        def exportJobExecutor = new ExportJobExecutor()
        return exportJobExecutor.getExportJobFileStream(job.viewerURL)
    }

    def exportData(Map jobDataMap, ZipOutputStream output) {

        List<Map> dataTypeAndFormatList = jobDataMap.dataTypeAndFormatList.flatten()
        org.transmartproject.core.users.User user = jobDataMap.user
        Constraint constraint = jobDataMap.constraint

        dataTypeAndFormatList.each { typeFormatPair ->
            Format outFormat = from(typeFormatPair.format)
            if (!supportedFileFormats.contains(outFormat)) {
                throw new InvalidRequestException("Export for ${outFormat} format is not supported.")
            }
            String dataType = typeFormatPair.dataType
            String dataView = typeFormatPair.dataView ?: grailsApplication.config.export.clinical.defaultDataView

            if (dataType == 'clinical') {
                try {
                    multidimensionalDataService.writeClinical(outFormat, constraint, user, output, dataView)
                } catch (LegacyStudyException e) {
                    throw new InvalidRequestException("This endpoint does not support legacy studies.", e)
                }
            } else {
                try {
                    multidimensionalDataService.writeHighdim(outFormat, dataType, constraint, null, null, user, output, dataView)
                } catch (LegacyStudyException e) {
                    throw new InvalidRequestException("This endpoint does not support legacy studies.", e)
                }
            }
        }
    }
}
