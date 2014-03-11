package org.transmartproject.db.querytool

import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.querytool.QueryStatus
import org.transmartproject.db.i2b2data.PatientDimension

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

    static hasMany = [patientSet: QtPatientSetCollection,
                      patients:   PatientDimension]

	static mapping = {
        table          schema: 'I2B2DEMODATA'

        /* use sequence instead of identity because our Oracle schema doesn't
         * have a trigger that fills the column in this case */
        id             column: 'result_instance_id', generator: 'sequence',
                       params: [sequence: 'qt_sq_qri_qriid']
        errorMessage   column: 'message'
        queryInstance  column: 'query_instance_id'

        patients       joinTable: [name:   'qt_patient_set_collection',
                                   key:    'result_instance_id',
                                   column: 'patient_num']

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
