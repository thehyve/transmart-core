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

package org.transmartproject.db.dataquery.highdim.rbm

class DeRbmAnnotation {

    String gplId
    String antigenName
    String uniprotId
    String uniprotName
    String geneSymbol
    String geneId

    static mapping = {
        table   schema:    'deapp',   name: 'de_rbm_annotation'
        id      generator: 'assigned'
        version false
    }

    static constraints = {
        gplId       maxSize:  50
        antigenName maxSize:  800
        uniprotId   nullable: true, maxSize: 200
        uniprotName nullable: true, maxSize: 200
        geneSymbol  nullable: true, maxSize: 200
        geneId      nullable: true, maxSize: 400
    }
}
