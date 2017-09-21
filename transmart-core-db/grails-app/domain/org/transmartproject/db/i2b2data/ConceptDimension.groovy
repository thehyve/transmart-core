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

package org.transmartproject.db.i2b2data

import groovy.transform.ToString
import org.transmartproject.core.concept.Concept

@ToString
class ConceptDimension implements Concept {

    String conceptPath
    String conceptCode
    String name

    // not used
    //String       conceptBlob
    //Date         updateDate
    //Date         downloadDate
    //Date         importDate
    //String       sourcesystemCd
    //BigDecimal   uploadId

    static mapping = {
        table   schema: 'i2b2demodata'
        id      name:   'conceptPath', generator: 'assigned'

        conceptPath column: 'concept_path'
        conceptCode column: 'concept_cd'
        name        column: 'name_char'

        version false
    }

    static constraints = {
        conceptPath     maxSize:    700
        conceptCode     maxSize:    50
        name            nullable:   true,   maxSize:   2000

        // not used
        //conceptBlob      nullable:   true
        //updateDate       nullable:   true
        //downloadDate     nullable:   true
        //importDate       nullable:   true
        //sourcesystemCd   nullable:   true,   maxSize:   50
        //uploadId         nullable:   true
    }
}
