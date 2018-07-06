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

package org.transmartproject.db

import org.slf4j.LoggerFactory

/**
 * Helper class for dealing with test data.
 */
class TestDataHelper {

    static List<String> getMissingValueFields(Object obj, Collection<String> fields) {
        def props = obj.class.metaClass.properties.findAll { fields.contains(it.name) }
        props.findAll({ !it.getProperty(obj) }).collect({ it.name })
    }

    static void save(Collection objects) {
        if (objects == null) {
            return //shortcut for no objects to save
        }

        objects.forEach({ object ->
            object.save(flush: true)
            LoggerFactory.getLogger(TestDataHelper).info "Saved ${object.class.simpleName}: ${object}"
        })
    }

}
