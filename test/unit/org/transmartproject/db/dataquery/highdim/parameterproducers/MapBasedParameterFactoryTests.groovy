package org.transmartproject.db.dataquery.highdim.parameterproducers

import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.projections.Projection

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class MapBasedParameterFactoryTests {

    private DataRetrievalParameterFactory testee

    @Before
    void setUp() {
        testee = new MapBasedParameterFactory(
                'test_projection': { Map params ->
                    { it ->
                        [ it, params ]
                    } as Projection
                }
        )
    }

    @Test
    void testSupportedNames() {
        assertThat testee.supportedNames, contains(
                equalTo('test_projection'),
        )
    }

    @Test
    void testSupports() {
        assertThat testee.supports('test_projection'), is(true)
        assertThat testee.supports('foobar'), is(false)
    }

    @Test
    void testCreateFromParams() {
        LinkedHashMap<String, Integer> paramsMap = ['bogusParam': 42]
        def bogusProjection = testee.createFromParameters('test_projection', paramsMap)
        assertThat bogusProjection, isA(Projection)
        assertThat bogusProjection.doWithResult('bogus'), is(equalTo([ 'bogus', paramsMap ]))
    }

}
