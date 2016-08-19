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
import org.transmartproject.db.dataquery.highdim.projections.SimpleRealProjection

class SimpleRealProjectionsFactory implements DataRetrievalParameterFactory {

    /* projection name -> property */
    Map<String, String> projectionToProperty

    SimpleRealProjectionsFactory(Map<String, String> projectionToProperty) {
        this.projectionToProperty = projectionToProperty
    }

    @Override
    Set<String> getSupportedNames() {
        projectionToProperty.keySet()
    }

    @Override
    boolean supports(String name) {
        projectionToProperty.containsKey(name)
    }

    @Override
    def createFromParameters(String name,
                             Map<String, Object> params,
                             Object createParameter) {
        if (!supports(name)) {
            return null
        }

        if (!params.isEmpty()) {
            throw new InvalidArgumentsException(
                    'This projection takes no parameters')
        }

        new SimpleRealProjection(property: projectionToProperty[name])
    }
}
