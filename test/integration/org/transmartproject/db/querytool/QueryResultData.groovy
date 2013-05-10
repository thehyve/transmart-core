package org.transmartproject.db.querytool

import org.transmartproject.core.querytool.QueryStatus
import org.transmartproject.db.i2b2data.PatientDimension

class QueryResultData {

    static QtQueryMaster createQueryResult(List<PatientDimension> patients) {
        QtQueryMaster queryMaster = new QtQueryMaster(
                name       : 'test-fake-query-1',
                userId     : 'fake-user',
                groupId    : 'fake group',
                createDate : new Date(),
                requestXml : ''
        )

        QtQueryInstance queryInstance = new QtQueryInstance(
                userId       : 'fake-user',
                groupId      : 'fake group',
                startDate    : new Date(),
                statusTypeId : QueryStatus.COMPLETED.id,
                queryMaster  : queryMaster,
        )
        queryMaster.addToQueryInstances(queryInstance)

        QtQueryResultInstance resultInstance = new QtQueryResultInstance(
                statusTypeId  : QueryStatus.FINISHED.id,
                startDate     : new Date(),
                queryInstance : queryInstance,
                setSize       : patients.size(),
                realSetSize   : patients.size(),
        )
        queryInstance.addToQueryResults(resultInstance)

        def i = 0;
        patients.each { patient ->
            resultInstance.addToPatientSet(new QtPatientSetCollection(
                    setIndex: i++,
                    patient: patient
            ))
        }

        queryMaster
    }

}
