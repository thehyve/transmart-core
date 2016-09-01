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
        table          schema: 'I2B2DEMODATA'

        /* use sequence instead of identity because our Oracle schema doesn't
         * have a trigger that fills the column in this case */
        id             column: 'query_master_id', generator: 'sequence',
                       params: [sequence: 'qt_sq_qm_qmid', schema: 'i2b2demodata']
        generatedSql   type:   'text'
        requestXml     type:   'text'
        i2b2RequestXml column: 'I2B2_REQUEST_XML', type: 'text'
        version false
    }

    static constraints = {
        name           maxSize:  250
        userId         maxSize:  50
        groupId        maxSize:  50
        masterTypeCd   nullable: true, maxSize: 2000
        pluginId       nullable: true
        deleteDate     nullable: true
        deleteFlag     nullable: true, maxSize: 3
        generatedSql   nullable: true
        requestXml     nullable: true
        i2b2RequestXml nullable: true
    }

    @Override
    String toString() {
        getClass().canonicalName + "[${attached?'attached':'not attached'}" +
                "] [ id=$id, name=$name, createDate=$createDate ]"
    }
}
