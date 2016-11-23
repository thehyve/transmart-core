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

package org.transmartproject.db.ontology

class Birn extends AbstractI2b2Metadata implements Serializable {

    static String backingTable = 'BIRN'

    static mapping = {
        table         name: 'BIRN', schema: 'I2B2METADATA'
        version       false

        /* hibernate needs an id, see
         * http://docs.jboss.org/hibernate/orm/3.3/reference/en/html/mapping.html#mapping-declaration-id
         */
        id          composite: ['fullName', 'name']

        AbstractI2b2Metadata.mapping.delegate = delegate
        AbstractI2b2Metadata.mapping()
	}

	static constraints = {
        cSynonymCd          nullable:   false

        AbstractI2b2Metadata.constraints.delegate = delegate
        AbstractI2b2Metadata.constraints()
	}

}
