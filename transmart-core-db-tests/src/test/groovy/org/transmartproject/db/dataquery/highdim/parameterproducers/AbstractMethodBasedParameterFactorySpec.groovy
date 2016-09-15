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
class AbstractMethodBasedParameterFactorySpec extends Specification {

    private DataRetrievalParameterFactory testee

    static class TestMethodBasedParameterFactory
            extends AbstractMethodBasedParameterFactory {

        @ProducerFor('made_up_projection_1')
        Projection createMadeUpProjection1(Map<String, Object> params) {
            ({ [ '1', params ] } as Projection)
        }

        @ProducerFor('made_up_projection_2')
        Projection createMadeUpProjection2(
                Map<String, Object> params, Object createProducer) {
            ({ [ '2', params, createProducer ] } as Projection)
        }
    }

    void setup() {
        testee = new TestMethodBasedParameterFactory()
    }

    void testSupportedNames() {
        expect: testee.supportedNames containsInAnyOrder(
                equalTo('made_up_projection_1'),
                equalTo('made_up_projection_2'),
        )
    }

    void testSupports() {
        expect:
            testee.supports('made_up_projection_1') is(true)
            testee.supports('foobar') is(false)
    }

    void testCreateFromParams() {
        LinkedHashMap<String, Integer> paramsMap = [a: 1]
        def bogusProjection = testee.createFromParameters('made_up_projection_1', paramsMap, {})
        expect:
            bogusProjection isA(Projection)
            bogusProjection.doWithResult('bogus') is(equalTo([ '1', paramsMap ]))
    }

    void testCreateFromParamsTwoArgProducer() {
        LinkedHashMap<String, Integer> paramsMap = [a: 1]
        Closure<Void> objectCreate = { -> }
        def bogusProjection = testee.createFromParameters('made_up_projection_2', paramsMap, objectCreate)
        expect:
            bogusProjection isA(Projection)
            bogusProjection.doWithResult('bogus') is(equalTo([ '2', paramsMap, objectCreate ]))
    }

    void testCreateFromParamsUnsupported() {
        expect: testee.createFromParameters('foobar', [:], {}) is(nullValue())
    }
}
