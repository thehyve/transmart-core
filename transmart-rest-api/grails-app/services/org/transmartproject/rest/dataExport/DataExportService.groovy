package org.transmartproject.rest.dataExport

import grails.transaction.Transactional
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.LegacyStudyException
import org.transmartproject.core.users.ProtectedOperation
import org.transmartproject.core.users.User
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.OrConstraint
import org.transmartproject.db.multidimquery.query.PatientSetConstraint
import org.transmartproject.rest.MultidimensionalDataService
import org.transmartproject.rest.MultidimensionalDataService.Format as Format
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import java.util.zip.ZipOutputStream


@Transactional
class DataExportService {

    MultidimensionalDataService multidimensionalDataService

    private static Constraint patientSetConstraint(List<Long> ids) {
        new OrConstraint(args:  ids.collect { new PatientSetConstraint(patientSetId: it) } )
    }

    List highDimDataTypesForPatientSets(List<Long> patientSetIds, User user) {
        def constraint = patientSetConstraint(patientSetIds)
        highDimDataTypes(constraint, user)
    }

    List highDimDataTypesForObservationSets(List patientSetIds, User user) {
        //TODO saving observationSets is not supported yet
        throw new NotImplementedException()
    }

    private List highDimDataTypes(Constraint constraint, User user) {
        multidimensionalDataService.multiDimService.retriveHighDimDataTypes(constraint, user)
    }

    List patientSetsExportPermission(List<Long> ids, User user){
        ids.collect { setId ->
            multidimensionalDataService.multiDimService.findPatientSet(setId, user, ProtectedOperation.WellKnownOperations.EXPORT)
        }
    }

    def exportData(Map jobDataMap, ZipOutputStream output) {

        List<Map> dataTypeAndFormatList = jobDataMap.dataTypeAndFormatList.flatten()
        User user = jobDataMap.user
        Constraint constraint = patientSetConstraint(jobDataMap.ids)

        dataTypeAndFormatList.each { typeFormatPair ->
            Format outFormat = Format[typeFormatPair.format]
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

