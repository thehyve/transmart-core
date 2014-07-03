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

class SearchKeywordCoreDb {

	String keyword
    /* usage patterns shows joins of bioDataId with:
     * search_bio_mkr_correl_fast_mv.domain_object_id
     * search_bio_mkr_correl_view.domain_object_id
     * bio_marker_correl_mv.bio_marker_id
     */
	Long   bioDataId
	String uniqueId            /* for genes: GENE: primary_external_id (in bio_marker) */
	String dataCategory
	String displayDataCategory

    // Do not map this point (though they exist in the database):
    //String sourceCode
	//BigDecimal ownerAuthUserId

	static mapping = {
        table   schema: 'searchapp',         name:     'search_keyword'
		id      column: 'search_keyword_id', generator: 'assigned'
		version false
	}

	static constraints = {
        keyword             nullable: true,  maxSize: 400
        bioDataId           nullable: true
        uniqueId            maxSize:  1000
        dataCategory        maxSize:  400,   unique: 'uniqueId'
        displayDataCategory nullable: true,  maxSize: 400

        // see above:
        //sourceCode        nullable: true,  maxSize: 200
        //ownerAuthUserId   nullable: true
	}
}
