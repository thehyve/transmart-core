package org.transmartproject.db.querytool

class QtQueryMaster {

    Long     id
    String   name
    String   userId
    String   groupId
    String   masterTypeCd
    Long     pluginId
    Date     createDate
    Date     deleteDate
    String   deleteFlag = 'N'/* 'N'/'Y' */
    String   generatedSql
    String   requestXml
    String   i2b2RequestXml

	static hasMany = [queryInstances: QtQueryInstance]

	static mapping = {
        table           schema: 'I2B2DEMODATA'

        /* use sequence instead of identity because our Oracle schema doesn't
         * have a trigger that fills the column in this case */
        id              column: 'query_master_id', generator: 'sequence',
                        params: [sequence: 'qt_sq_qm_qmid', schema: 'i2b2demodata']
        generatedSql    type:   'text'
        requestXml      type:   'text'
        i2b2RequestXml  column: 'I2B2_REQUEST_XML', type: 'text'
		version false
	}

	static constraints = {
        name             maxSize:    250
        userId           maxSize:    50
        groupId          maxSize:    50
        masterTypeCd     nullable:   true,   maxSize:   2000
        pluginId         nullable:   true
        deleteDate       nullable:   true
        deleteFlag       nullable:   true,   maxSize:   3
        generatedSql     nullable:   true
        requestXml       nullable:   true
        i2b2RequestXml   nullable:   true
	}

    @Override
    String toString() {
        getClass().canonicalName + "[${attached?'attached':'not attached'}" +
                "] [ id=$id, name=$name, createDate=$createDate ]"
    }
}
