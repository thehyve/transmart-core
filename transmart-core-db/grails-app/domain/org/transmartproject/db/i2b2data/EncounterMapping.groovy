/*
 * Copyright © 2019 The Hyve B.V.
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

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = 'id')
class EncounterMapping implements Serializable {

    String encryptedId
    String source
    VisitDimension visit

    static mapping = {
        table name: 'encounter_mapping', schema: 'i2b2demodata'
        encryptedId column: 'encounter_ide'
        source column: 'encounter_ide_source'
        id composite: ['encryptedId', 'source']
        visit column: 'encounter_num'
        version false
    }

}
