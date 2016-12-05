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

package org.transmartproject.core.concept

import groovy.transform.EqualsAndHashCode

/*
 * A concept key is made of two parts: \\c_table_cd\c_fullname
 *
 * The c_table_cd describes the metadata table wherein the concept is stored,
 * while c_fullname is a unique identifier for the concept itself.
 */
@EqualsAndHashCode
class ConceptKey {

    final String tableCode
    final ConceptFullName conceptFullName

    ConceptKey(String key) {
        if (key == null)
            throw new IllegalArgumentException('Concept key is null')
        if (key.size() < 5 /* \\a\b */)
            throw new IllegalArgumentException("Concept key is too short")
        if (key[0..1] != '\\\\')
            throw new IllegalArgumentException('Concept key must start with ' +
                    '\\\\')

        def matcher = (key =~ '\\\\')
        if (!matcher.find(3))
            throw new IllegalArgumentException('No \\ character found after ' +
                    'position 2')

        tableCode = key.substring(2, matcher.start())
        conceptFullName = new ConceptFullName(key.substring(matcher.start()))
    }

    ConceptKey(String tableCode, String fullName) {
        if (tableCode == null || fullName == null)
            throw new IllegalArgumentException('Table code or full name are ' +
                    'null')
        if (tableCode.find('\\\\') != null)
            throw new IllegalArgumentException("Table code includes a " +
                    "back slash")
        this.tableCode = tableCode
        conceptFullName = new ConceptFullName(fullName)
    }

    @Override
    public String toString() {
        return "\\\\" + tableCode + conceptFullName
    }
}
