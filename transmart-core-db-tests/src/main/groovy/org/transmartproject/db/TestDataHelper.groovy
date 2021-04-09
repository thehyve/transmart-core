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
import org.transmartproject.core.exceptions.UnexpectedResultException

/**
 * Helper class for dealing with test data.
 */
class TestDataHelper {

    static void save(Collection objects) {
        if (objects == null) {
            return //shortcut for no objects to save
        }

        objects.forEach({ object ->
            try {
                object.save(flush: true, failOnError: true)
                LoggerFactory.getLogger(TestDataHelper).debug "Saved ${object.class.simpleName}: ${object}"
            } catch (Exception e) {
                LoggerFactory.getLogger(TestDataHelper).error "Error while saving ${object.class.simpleName}: ${object}"
                throw new UnexpectedResultException("Cannot save object of type ${object.class.simpleName}", e)
            }
        })
    }

}
