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

import org.transmartproject.core.dataquery.highdim.projections.Projection
import spock.lang.Specification

import static org.hamcrest.Matchers.*

class MapBasedParameterFactorySpec extends Specification {

    private DataRetrievalParameterFactory testee

    void setup() {
        testee = new MapBasedParameterFactory(
                'test_projection': { Map params ->
                    { it ->
                        [it, params]
                    } as Projection
                },
                'test_projection_2': { Map params, createProjection ->
                    { it ->
                        [it, params, createProjection]
                    } as Projection
                },
        )
    }

    void testSupportedNames() {
        expect:
        testee.supportedNames.size() == 2
        'test_projection' in testee.supportedNames
        'test_projection_2' in testee.supportedNames
    }

    void testSupports() {
        expect:
        testee.supports('test_projection')
        testee.supports('test_projection_2')
        !testee.supports('foobar')
    }

    void testCreateFromParams() {
        LinkedHashMap<String, Integer> paramsMap = ['bogusParam': 42]
        def bogusProjection = testee.createFromParameters(
                'test_projection', paramsMap, { -> })

        expect:
        bogusProjection isA(Projection)
        bogusProjection.doWithResult('bogus') == ['bogus', paramsMap]
    }

    void testCreateFromParamsTwoArgClosure() {
        LinkedHashMap<String, Integer> paramsMap = ['bogusParam': 42]
        Closure createProjection = { String name, Map params -> }

        def bogusProjection = testee.createFromParameters(
                'test_projection_2', paramsMap, createProjection)

        expect:
        bogusProjection instanceof Projection
        bogusProjection.doWithResult('bogus') == ['bogus', paramsMap, createProjection]
    }

}
