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
import org.transmartproject.rest.serialization.DataSerializer
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
        getDataSerializerByDataView(dataView).supportedFormats
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
            DataSerializer dataSerializer = getDataSerializerByDataView(element.dataView)
            if (!dataSerializer.supportedFormats.contains(element.format)) {
                throw new InvalidRequestException("Export for ${element.format} format is not supported.")
            }

            if (element.dataType == 'clinical') {
                try {
                    switch(element.dataView) {
                        case DataView.DATA_TABLE:
                            TableConfig tableConfig = jobDataMap.tableConfig
                            dataSerializer.writeTable(element.format, constraint, tableConfig, user, output)
                            break
                        case DataView.SURVEY_TABLE:
                            DataRetrievalParameters parameters = new DataRetrievalParameters(
                                    constraint: constraint,
                                    includeMeasurementDateColumns: jobDataMap.includeMeasurementDateColumns,
                                    exportFileName: fileName
                            )
                            dataSerializer.writeClinical(element.format, parameters, user, output)
                            break
                        default:
                            throw new InvalidRequestException("Data view not supported.")
                    }
                } catch (LegacyStudyException e) {
                    throw new InvalidRequestException("This endpoint does not support legacy studies.", e)
                }
            } else {
                try {
                    dataSerializer.writeHighdim(element.format, element.dataType, constraint, null, null, user, output)
                } catch (LegacyStudyException e) {
                    throw new InvalidRequestException("This endpoint does not support legacy studies.", e)
                }
            }
        }
    }

    DataSerializer getDataSerializerByDataView(DataView dataView) {
        switch(dataView) {
            case DataView.SURVEY_TABLE:
                return surveyTableViewDataSerializationService
            case DataView.DATA_TABLE:
                return dataTableViewDataSerializationService
            default:
                throw new InvalidRequestException("Data view not supported: ${dataView}")
        }
    }

}
