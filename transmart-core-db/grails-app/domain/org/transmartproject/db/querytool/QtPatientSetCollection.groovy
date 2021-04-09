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

import grails.util.Environment
import org.transmartproject.db.i2b2data.PatientDimension

class QtPatientSetCollection {

	Long            setIndex

    static belongsTo = [
            resultInstance: QtQueryResultInstance,
            patient:        PatientDimension,
    ]

	static mapping = {
        table          schema:   'I2B2DEMODATA'

        id             column:   'patient_set_coll_id', type: Long,
                       generator: Environment.current == Environment.TEST ? 'identity' : 'sequence',
                       params: [sequence: 'qt_patient_set_collection_patient_set_coll_id_seq', schema: 'i2b2demodata']
        resultInstance column:   'result_instance_id'
        patient        column:   'patient_num'

        sort           setIndex: 'asc'

		version false
	}

	static constraints = {
        resultInstance   nullable: true
		setIndex         nullable: true
		patient          nullable: true
	}
}
