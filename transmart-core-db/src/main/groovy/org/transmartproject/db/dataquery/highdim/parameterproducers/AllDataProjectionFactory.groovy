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

import com.google.common.collect.ImmutableMap
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.projections.AllDataProjectionImpl

class AllDataProjectionFactory implements DataRetrievalParameterFactory {

    private Map<String, Class> dataProperties
    private Map<String, Class> rowProperties

    AllDataProjectionFactory(Map<String, Class> dataProperties, Map<String, Class> rowProperties) {
        this.dataProperties = dataProperties
        this.rowProperties = rowProperties
    }

    @Override
    Set<String> getSupportedNames() {
        [Projection.ALL_DATA_PROJECTION] as Set
    }

    @Override
    boolean supports(String name) {
        Projection.ALL_DATA_PROJECTION == name
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

        new AllDataProjectionImpl(dataProperties, rowProperties)
    }
}
