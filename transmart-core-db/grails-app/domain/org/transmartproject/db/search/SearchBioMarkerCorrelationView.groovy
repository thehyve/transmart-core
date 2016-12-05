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

package org.transmartproject.db.search

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = [ 'domainObjectId', 'associatedBioMarkerId', 'name', 'valueMetric', 'mvId' ])
class SearchBioMarkerCorrelationView implements Serializable {

    //is a view!

	Long domainObjectId
	Long associatedBioMarkerId
	String correlationType
	Long valueMetric
	Long mvId

	static mapping = {
        table                 schema:    'searchapp', name: 'search_bio_mkr_correl_view'

        id                    composite: ['domainObjectId',   'associatedBioMarkerId', 'correlationType', 'valueMetric', 'mvId']

        associatedBioMarkerId column:    'asso_bio_marker_id'
        correlationType       column:    'correl_type'

        version               false
	}

	static constraints = {
        domainObjectId        nullable: true
        associatedBioMarkerId nullable: true
        correlationType       nullable: true, maxSize: 19
        valueMetric           nullable: true
        mvId                  nullable: true
	}
}
