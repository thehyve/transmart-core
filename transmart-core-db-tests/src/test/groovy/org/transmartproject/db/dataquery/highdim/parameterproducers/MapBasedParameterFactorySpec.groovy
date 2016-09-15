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

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import groovy.util.logging.Slf4j
import spock.lang.Specification

import org.transmartproject.core.dataquery.highdim.projections.Projection

import static org.hamcrest.Matchers.*

@Integration
@Rollback
@Slf4j
class MapBasedParameterFactorySpec extends Specification {

    private DataRetrievalParameterFactory testee

    void setup() {
        testee = new MapBasedParameterFactory(
                'test_projection': { Map params ->
                    { it ->
                        [ it, params ]
                    } as Projection
                },
                'test_projection_2': { Map params, createProjection ->
                    { it ->
                        [ it, params, createProjection ]
                    } as Projection
                },
        )
    }

    void testSupportedNames() {
        expect: testee.supportedNames contains(
                equalTo('test_projection'),
                equalTo('test_projection_2'),
        )
    }

    void testSupports() {
        expect:
            testee.supports('test_projection') is(true)
            testee.supports('test_projection_2') is(true)
            testee.supports('foobar') is(false)
    }

    void testCreateFromParams() {
        LinkedHashMap<String, Integer> paramsMap = ['bogusParam': 42]
        def bogusProjection = testee.createFromParameters(
                'test_projection', paramsMap, { -> })

        expect:
            bogusProjection isA(Projection)
            bogusProjection.doWithResult('bogus') is(equalTo([ 'bogus', paramsMap ]))
    }

    void testCreateFromParamsTwoArgClosure() {
        LinkedHashMap<String, Integer> paramsMap = ['bogusParam': 42]
        Closure createProjection = { String name, Map params -> }

        def bogusProjection = testee.createFromParameters(
                'test_projection_2', paramsMap, createProjection)

        expect:
            bogusProjection isA(Projection)
            bogusProjection.doWithResult('bogus')
                is(equalTo([ 'bogus', paramsMap, createProjection ]))
    }

}
