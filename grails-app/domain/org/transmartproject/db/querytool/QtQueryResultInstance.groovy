package org.transmartproject.db.querytool

import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.querytool.QueryStatus

class QtQueryResultInstance implements QueryResult {

    Short             resultTypeId = 1
    Long              setSize
    Date              startDate
    Date              endDate
    String            deleteFlag = 'N'
    Short             statusTypeId
    String            errorMessage
    String            description
    Long              realSetSize
    String            obfuscMethod
    QtQueryInstance   queryInstance

	static belongsTo = QtQueryInstance

    static hasMany = [patientSet: QtPatientSetCollection]

	static mapping = {
        table          schema: 'I2B2DEMODATA'

        id             column: "result_instance_id", generator: "identity"
        errorMessage   column: 'message'
        queryInstance  column: 'query_instance_id'

		version false
	}

	static constraints = {
        setSize        nullable:   true
        endDate        nullable:   true
        deleteFlag     nullable:   true,   maxSize:   3
        errorMessage   nullable:   true
        description    nullable:   true,   maxSize:   200
        realSetSize    nullable:   true
        obfuscMethod   nullable:   true,   maxSize:   500
	}

    @Override
    QueryStatus getStatus() {
        QueryStatus.forId(statusTypeId)
    }
}
