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

import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.projections.Projection

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class AbstractMethodBasedParameterFactoryTests {

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

    @Before
    void setUp() {
        testee = new TestMethodBasedParameterFactory()
    }

    @Test
    void testSupportedNames() {
        assertThat testee.supportedNames, containsInAnyOrder(
                equalTo('made_up_projection_1'),
                equalTo('made_up_projection_2'),
        )
    }

    @Test
    void testSupports() {
        assertThat testee.supports('made_up_projection_1'), is(true)
        assertThat testee.supports('foobar'), is(false)
    }

    @Test
    void testCreateFromParams() {
        LinkedHashMap<String, Integer> paramsMap = [a: 1]
        def bogusProjection = testee.createFromParameters('made_up_projection_1', paramsMap, {})
        assertThat bogusProjection, isA(Projection)
        assertThat bogusProjection.doWithResult('bogus'), is(equalTo([ '1', paramsMap ]))
    }

    @Test
    void testCreateFromParamsTwoArgProducer() {
        LinkedHashMap<String, Integer> paramsMap = [a: 1]
        Closure<Void> objectCreate = { -> }
        def bogusProjection = testee.createFromParameters('made_up_projection_2', paramsMap, objectCreate)
        assertThat bogusProjection, isA(Projection)
        assertThat bogusProjection.doWithResult('bogus'), is(equalTo([ '2', paramsMap, objectCreate ]))
    }

    @Test
    void testCreateFromParamsUnsupported() {
        assertThat testee.createFromParameters('foobar', [:], {}), is(nullValue())
    }
}
