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

class I2b2Secure extends AbstractI2b2Metadata implements Serializable {

    String secureObjectToken

    static String backingTable = 'I2B2_SECURE'

    static mapping = {
        table         name: 'I2B2_SECURE', schema: 'I2B2METADATA'
        version       false

        id composite: ['fullName', 'name']

        secureObjectToken column: 'secure_obj_token'

        AbstractI2b2Metadata.mapping.delegate = delegate
        AbstractI2b2Metadata.mapping()
    }

    static constraints = {
        cSynonymCd          nullable:   false

        AbstractI2b2Metadata.constraints.delegate = delegate
        AbstractI2b2Metadata.constraints()
    }

}
