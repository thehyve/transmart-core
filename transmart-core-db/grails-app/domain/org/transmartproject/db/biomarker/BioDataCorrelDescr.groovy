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

package org.transmartproject.db.biomarker

class BioDataCorrelDescr {

    String correlation /* name */
    String description
    String typeName
    String status
    String source
    String sourceCode

    static hasMany = [ correlationRows: BioDataCorrelationCoreDb ]

    static mapping = {
        table   name:   'bio_data_correl_descr',    schema:    'biomart'
        id      column: 'bio_data_correl_descr_id', generator: 'assigned'
        version false
    }

    static constraints = {
        correlation nullable: true, maxSize: 1020
        description nullable: true, maxSize: 2000
        typeName    nullable: true, maxSize: 400
        status      nullable: true, maxSize: 400
        source      nullable: true, maxSize: 200
        sourceCode  nullable: true, maxSize: 400
    }
}
