package org.transmartproject.rest.dataExport

import grails.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.core.multidimquery.datatable.TableConfig
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.LegacyStudyException
import org.transmartproject.core.multidimquery.export.DataRetrievalParameters
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.users.User
import org.transmartproject.db.job.AsyncJobCoreDb
import org.transmartproject.rest.HypercubeDataSerializationService
import org.transmartproject.rest.SurveyTableViewDataSerializationService
import org.transmartproject.rest.serialization.DataSerializer
import org.transmartproject.core.multidimquery.export.ExportElement
import org.transmartproject.core.multidimquery.export.Format

import java.util.zip.ZipOutputStream

import static Format.SPSS
import static Format.TSV

@Transactional
@Component("restExportService")
class ExportService {

    @Autowired
    HypercubeDataSerializationService hypercubeDataSerializationService

    Set<Format> exportFormats =  EnumSet.of(TSV, SPSS)

    @Autowired
    SurveyTableViewDataSerializationService surveyTableViewDataSerializationService

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
                    if (jobDataMap.tableConfig) {
                        TableConfig tableConfig = jobDataMap.tableConfig
                        dataSerializer.writeTable(element.format, constraint, tableConfig, user, output)
                    } else {
                        DataRetrievalParameters parameters = new DataRetrievalParameters(
                                constraint: constraint,
                                includeMeasurementDateColumns: jobDataMap.includeMeasurementDateColumns,
                                exportFileName: fileName
                        )
                        dataSerializer.writeClinical(element.format, parameters, user, output)
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

    DataSerializer getDataSerializerByDataView(String dataView) {
        if (dataView == 'surveyTable') {
            surveyTableViewDataSerializationService
        } else {
            hypercubeDataSerializationService
        }
    }
}
