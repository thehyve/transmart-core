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
        Projection createMadeUpProjection2(Map<String, Object> params) {
            ({ [ '2', params ] } as Projection)

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
        def bogusProjection = testee.createFromParameters('made_up_projection_1', paramsMap)
        assertThat bogusProjection, isA(Projection)
        assertThat bogusProjection.doWithResult('bogus'), is(equalTo([ '1', paramsMap ]))
    }

    @Test
    void testCreateFromParamsUnsupported() {
        assertThat testee.createFromParameters('foobar', [:]), is(nullValue())
    }
}
