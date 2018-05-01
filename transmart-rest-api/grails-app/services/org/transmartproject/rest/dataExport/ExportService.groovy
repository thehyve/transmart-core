package org.transmartproject.rest.dataExport

import grails.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.LegacyStudyException
import org.transmartproject.db.job.AsyncJobCoreDb
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.rest.DataTableViewDataSerializationService
import org.transmartproject.rest.HypercubeDataSerializationService
import org.transmartproject.rest.SurveyTableViewDataSerializationService
import org.transmartproject.rest.serialization.DataSerializer
import org.transmartproject.rest.serialization.Format

import java.util.zip.ZipOutputStream

import static org.transmartproject.rest.serialization.Format.*

@Transactional
@Component("restExportService")
class ExportService {

    @Autowired
    HypercubeDataSerializationService hypercubeDataSerializationService

    Set<Format> exportFormats =  EnumSet.of(TSV, SPSS)

    @Autowired
    SurveyTableViewDataSerializationService surveyTableViewDataSerializationService

    @Autowired
    DataTableViewDataSerializationService dataTableViewDataSerializationService

    Set<Format> getSupportedFormats(String dataView) {
        Set<Format> supportedExportFormats = new LinkedHashSet<Format>(exportFormats)
        supportedExportFormats.retainAll(getDataSerializerByDataView(dataView).supportedFormats)
        return supportedExportFormats
    }

    def downloadFile(AsyncJobCoreDb job) {
        if (job.jobStatus != JobStatus.COMPLETED.value) {
            throw new InvalidRequestException("Job with a name is not completed. Current status: '$job.jobStatus'")
        }

        def exportJobExecutor = new ExportJobExecutor()
        return exportJobExecutor.getExportJobFileStream(job.viewerURL)
    }

    def exportData(Map jobDataMap, OutputStream output) {

        List<Map> dataTypeAndFormatList = jobDataMap.dataTypeAndFormatList.flatten()
        org.transmartproject.core.users.User user = jobDataMap.user
        Constraint constraint = jobDataMap.constraint

        dataTypeAndFormatList.each { typeFormatPair ->
            Format outFormat = from(typeFormatPair.format)
            String dataType = typeFormatPair.dataType
            DataSerializer dataSerializer = getDataSerializerByDataView(typeFormatPair.dataView)
            if (!dataSerializer.supportedFormats.contains(outFormat)) {
                throw new InvalidRequestException("Export for ${outFormat} format is not supported.")
            }

            if (dataType == 'clinical') {
                try {
                    dataSerializer.writeClinical(outFormat, constraint, user, output, jobDataMap)
                } catch (LegacyStudyException e) {
                    throw new InvalidRequestException("This endpoint does not support legacy studies.", e)
                }
            } else {
                try {
                    dataSerializer.writeHighdim(outFormat, dataType, constraint, null, null, user, output)
                } catch (LegacyStudyException e) {
                    throw new InvalidRequestException("This endpoint does not support legacy studies.", e)
                }
            }
        }
    }

    DataSerializer getDataSerializerByDataView(String dataView) {
        if (dataView == 'surveyTable') {
            surveyTableViewDataSerializationService
        } else if (dataView == 'dataTable') {
            dataTableViewDataSerializationService
        } else {
            hypercubeDataSerializationService
        }
    }
}
