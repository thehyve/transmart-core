package org.transmartproject.rest.dataExport

import grails.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.core.multidimquery.datatable.TableConfig
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.LegacyStudyException
import org.transmartproject.core.multidimquery.DataRetrievalParameters
import org.transmartproject.core.multidimquery.export.DataView
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.users.User
import org.transmartproject.db.job.AsyncJobCoreDb
import org.transmartproject.rest.DataTableViewDataSerializationService
import org.transmartproject.rest.SurveyTableViewDataSerializationService
import org.transmartproject.core.multidimquery.export.ExportElement
import org.transmartproject.core.multidimquery.export.Format

import java.util.zip.ZipOutputStream

@Transactional
@Component("restExportService")
class ExportService {

    @Autowired
    SurveyTableViewDataSerializationService surveyTableViewDataSerializationService

    @Autowired
    DataTableViewDataSerializationService dataTableViewDataSerializationService

    Set<Format> getSupportedFormats(DataView dataView) {
        switch(dataView) {
            case DataView.SURVEY_TABLE:
                return surveyTableViewDataSerializationService.supportedFormats
            case DataView.DATA_TABLE:
                return [Format.TSV] as Set<Format>
            default:
                throw new InvalidRequestException("Data view not supported: ${dataView}")
        }
    }

    def downloadFile(AsyncJobCoreDb job) {
        if (job.jobStatus != JobStatus.COMPLETED.value) {
            throw new InvalidRequestException("Job with a name is not completed. Current status: '$job.jobStatus'")
        }

        def exportJobExecutor = new ExportJobExecutor()
        return exportJobExecutor.getExportJobFileStream(job.viewerURL)
    }

    def exportData(Map jobDataMap, String fileName, ZipOutputStream output) {

        List<ExportElement> dataTypeAndFormatList = jobDataMap.dataTypeAndFormatList
        User user = jobDataMap.user
        Constraint constraint = jobDataMap.constraint

        dataTypeAndFormatList.each { element ->
            if (element.dataType != 'clinical') {
                throw new InvalidRequestException("Export for data type ${element.dataType} is not supported.")
            }
            if (!getSupportedFormats(element.dataView).contains(element.format)) {
                throw new InvalidRequestException("Export for ${element.format} format is not supported.")
            }
            try {
                switch(element.dataView) {
                    case DataView.DATA_TABLE:
                        TableConfig tableConfig = jobDataMap.tableConfig
                        dataTableViewDataSerializationService.writeTableToTsv(constraint, tableConfig, user, output)
                        break
                    case DataView.SURVEY_TABLE:
                        DataRetrievalParameters parameters = new DataRetrievalParameters(
                                constraint: constraint,
                                includeMeasurementDateColumns: jobDataMap.includeMeasurementDateColumns,
                                exportFileName: fileName
                        )
                        surveyTableViewDataSerializationService.writeClinical(element.format, parameters, user, output)
                        break
                    default:
                        throw new InvalidRequestException("Data view not supported.")
                }
            } catch (LegacyStudyException e) {
                throw new InvalidRequestException("This endpoint does not support legacy studies.", e)
            }
        }
    }

}
