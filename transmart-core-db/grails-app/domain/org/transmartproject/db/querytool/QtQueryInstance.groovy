/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

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
