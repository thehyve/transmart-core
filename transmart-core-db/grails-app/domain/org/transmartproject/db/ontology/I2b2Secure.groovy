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

/**
 * Domain class for storing an ontology tree.
 */
class I2b2Secure extends AbstractI2b2Metadata implements Serializable {

    public static final String ROOT = '\\'

    /**
     * The source system code usually contains the study id
     * of the study the node belongs to.
     */
    @Deprecated
    String sourceSystemCd

    String secureObjectToken

    static String backingTable = 'I2B2_SECURE'

    static mapping = {
        table         name: 'I2B2_SECURE', schema: 'I2B2METADATA'
        version       false

        id composite: ['fullName', 'name']

        sourceSystemCd column: 'sourcesystem_cd'
        secureObjectToken column: 'secure_obj_token'

        AbstractI2b2Metadata.mapping.delegate = delegate
        AbstractI2b2Metadata.mapping()
    }

    static constraints = {
        sourceSystemCd      nullable: true
        cSynonymCd          nullable: false

        AbstractI2b2Metadata.constraints.delegate = delegate
        AbstractI2b2Metadata.constraints()
    }

}
