package org.transmartproject.rest.dataExport

import grails.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.LegacyStudyException
import org.transmartproject.db.job.AsyncJobCoreDb
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.rest.MultidimensionalDataService

import java.util.zip.ZipOutputStream

@Transactional
@Component("restExportService")
class ExportService {

    @Autowired
    MultidimensionalDataService multidimensionalDataService

    static enum FileFormat {
        TSV('TSV'), // TODO add support for other file formats

        final String value

        FileFormat(String value) { this.value = value }

        String toString() { value }

        String getKey() { name() }
    }

    static supportedFileFormats = FileFormat.values().collect { it.toString() }

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
            MultidimensionalDataService.Format outFormat = MultidimensionalDataService.Format[typeFormatPair.format]
            String dataType = typeFormatPair.dataType
            boolean tabular = typeFormatPair.tabular

            if (dataType == 'clinical') {
                try {
                    multidimensionalDataService.writeClinical([dataType : dataType, tabular: tabular], outFormat, constraint, user, output)
                } catch (LegacyStudyException e) {
                    throw new InvalidRequestException("This endpoint does not support legacy studies.", e)
                }
            } else {
                if (tabular) {
                    throw new UnsupportedOperationException('Tabular format is not supported for HD data.')
                }
                try {
                    multidimensionalDataService.writeHighdim(outFormat, dataType, constraint, null, null, user, output)
                } catch (LegacyStudyException e) {
                    throw new InvalidRequestException("This endpoint does not support legacy studies.", e)
                }
            }
        }
    }
}
