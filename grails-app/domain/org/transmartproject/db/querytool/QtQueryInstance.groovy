package org.transmartproject.db.querytool

class QtQueryInstance {

    String          userId
    String          groupId
    String          batchMode
    Date            startDate
    Date            endDate
    String          deleteFlag = 'N'
    Integer         statusTypeId
    String          message
    QtQueryMaster   queryMaster

	static hasMany = [queryResults: QtQueryResultInstance]
	static belongsTo = QtQueryMaster

	static mapping = {
        table       schema: 'I2B2DEMODATA'

        /* use sequence instead of identity because our Oracle schema doesn't
         * have a trigger that fills the column in this case */
		id          column: 'query_instance_id', generator: 'sequence',
                    params: [sequence: 'qt_sq_qi_qiid', schema: 'i2b2demodata']
        queryMaster column: 'query_master_id'
		version false
	}

	static constraints = {
        userId         maxSize:    50
        groupId        maxSize:    50
        batchMode      nullable:   true,   maxSize:   50
        endDate        nullable:   true
        deleteFlag     nullable:   true,   maxSize:   3
        statusTypeId   nullable:   true
        message        nullable:   true
	}
}
