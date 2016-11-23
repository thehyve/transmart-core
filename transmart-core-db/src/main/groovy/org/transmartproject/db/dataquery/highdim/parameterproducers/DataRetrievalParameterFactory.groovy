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

package org.transmartproject.db.dataquery.highdim.parameterproducers
/**
 * Defines the interface for beans implementing producers that take a constraint
 * or projection name and its parameters and create the corresponding constraint
 * or projection.
 */
interface DataRetrievalParameterFactory {

    Set<String> getSupportedNames()

    boolean supports(String name)

    /**
     * Returns the created object or null if the name is not recognized.
     * @param name
     * @param params
     * @param createParameter Callable object that can be used to create a
     * new constraint/projection. Takes the same parameters as the first
     * two of this method.
     * @return
     */
    def createFromParameters(String name,
                             Map<String, Object> params,
                             Object /* callable */ createParameter)

}
