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

import org.transmartproject.core.exceptions.InvalidArgumentsException

/**
 * Created by glopes on 11/18/13.
 */
class MapBasedParameterFactory implements DataRetrievalParameterFactory {

    private Map<String, Closure> producerMap

    /**
     * Constructor.
     * @param producerMap Takes a map between a data type name and a closure
     * that takes either 1) one argument, the parameters of the constraint/
     * projection or 2) two arguments, the parameters of the constraint/
     * projection and a callable object that allows the creation of the
     * constraints/projections. The closure should return the new constraint/
     * projection, never null. It may throw a {@link InvalidArgumentsException}.
     */
    MapBasedParameterFactory(Map<String, Closure> producerMap) {
        this.producerMap = producerMap
    }

    @Override
    Set<String> getSupportedNames() {
        producerMap.keySet()
    }

    @Override
    boolean supports(String name) {
        producerMap.containsKey name
    }

    @Override
    def createFromParameters(String name,
                             Map<String, Object> params,
                             Object createParameter) {
        Closure producer = producerMap[name]
        if (!producer) {
            return null
        }

        if (producer.maximumNumberOfParameters == 1) {
            producer.call params
        } else {
            producer.call params, createParameter
        }
    }
}
