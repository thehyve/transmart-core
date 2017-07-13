package org.transmartproject.rest.dataExport

import grails.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.LegacyStudyException
import org.transmartproject.core.users.ProtectedOperation
import org.transmartproject.db.job.AsyncJobCoreDb
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.OrConstraint
import org.transmartproject.db.multidimquery.query.PatientSetConstraint
import org.transmartproject.db.user.User
import org.transmartproject.rest.MultidimensionalDataService
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import javax.transaction.NotSupportedException
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

    static enum SupportedTypesOfSet {
        OBSERVATION('observation'),
        PATIENT('patient')

        final String value

        SupportedTypesOfSet(String value) { this.value = value }

        String toString() { value }

        String getKey() { name() }
    }

    static supportedFileFormats = FileFormat.values().collect { it.toString() }
    static supportedTypesOfSet = SupportedTypesOfSet.values().collect { it.toString() }

    static final clinicalDataType = "clinical"

    private static Constraint patientSetConstraint(List<Long> ids) {
        new OrConstraint(args:  ids.collect { new PatientSetConstraint(patientSetId: it) } )
    }

    def isUserAllowedToExport(List<Long> resultSetIds, User user, String typeOfSet) {
        if (typeOfSet == SupportedTypesOfSet.PATIENT.value) {
            patientSetsExportPermission(resultSetIds, user)
        }
    }

    List getDataFormats(String typeOfSet, List<Long> setIds, User user) {

        // clinical data (always included)
        List<String> dataFormats = []
        dataFormats.add(clinicalDataType)

        // highDim data
        switch (typeOfSet) {
            case SupportedTypesOfSet.PATIENT.value:
                List highDimDataTypes = highDimDataTypesForPatientSets(setIds, user)
                if (highDimDataTypes) dataFormats.addAll(highDimDataTypes)
                break
            case SupportedTypesOfSet.OBSERVATION.value:
                List highDimDataTypes = highDimDataTypesForObservationSets(setIds, user)
                if (highDimDataTypes) dataFormats.addAll(highDimDataTypes)
                break
            default:
                throw new NotSupportedException("Set type '$typeOfSet' not supported.")
        }

        dataFormats
    }

    List highDimDataTypesForPatientSets(List<Long> patientSetIds, org.transmartproject.core.users.User user) {
        def constraint = patientSetConstraint(patientSetIds)
        highDimDataTypes(constraint, user)
    }

    List highDimDataTypesForObservationSets(List patientSetIds, org.transmartproject.core.users.User user) {
        //TODO saving observationSets is not supported yet
        throw new NotImplementedException()
    }

    private List highDimDataTypes(Constraint constraint, org.transmartproject.core.users.User user) {
        multidimensionalDataService.multiDimService.retriveHighDimDataTypes(constraint, user)
    }

    List patientSetsExportPermission(List<Long> ids, org.transmartproject.core.users.User user){
        ids.collect { setId ->
            multidimensionalDataService.multiDimService.findPatientSet(setId, user)
        }
    }

    def downloadFile(AsyncJobCoreDb job) {
        if(job.jobStatus != JobStatus.COMPLETED.value) {
            throw new InvalidRequestException("Job with a name is not completed. Current status: '$job.jobStatus'")
        }
        File file = new File(job.viewerURL)
        if (!file.isFile()) {
            log.error("Failed to get the ZIP file. File does not exist in '$job.viewerURL'")
            throw new InvalidRequestException("File '$job.viewerURL' does not exists.")
        }
        return file

    }

    def exportData(Map jobDataMap, ZipOutputStream output) {

        List<Map> dataTypeAndFormatList = jobDataMap.dataTypeAndFormatList.flatten()
        org.transmartproject.core.users.User user = jobDataMap.user
        Constraint constraint = patientSetConstraint(jobDataMap.ids)

        dataTypeAndFormatList.each { typeFormatPair ->
            MultidimensionalDataService.Format outFormat = MultidimensionalDataService.Format[typeFormatPair.format]
            String dataType = typeFormatPair.dataType

            if (dataType == 'clinical') {
                try {
                    multidimensionalDataService.writeClinical([dataType : dataType], outFormat, constraint, user, output)
                } catch (LegacyStudyException e) {
                    throw new InvalidRequestException("This endpoint does not support legacy studies.", e)
                }
            } else {
                if (dataType in highDimDataTypes(constraint, user)) {
                    try {
                        multidimensionalDataService.writeHighdim(outFormat, dataType, constraint, null, null, user, output)
                    } catch (LegacyStudyException e) {
                        throw new InvalidRequestException("This endpoint does not support legacy studies.", e)
                    }
                } else {
                    throw new InvalidRequestException("Format '$dataType' is not supported")
                }
            }
        }
    }
}
