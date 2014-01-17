package org.transmartproject.db.dataquery.highdim

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*


@TestMixin(GrailsUnitTestMixin)
class DefaultHighDimensionTabularResultTests {

    @Test
    void testEquals() {
        def a = new DefaultHighDimensionTabularResult(closeSession: false),
            b = new DefaultHighDimensionTabularResult(closeSession: false)

        /* to avoid warning */
        a.close()
        b.close()

        assertThat a, is(not(equalTo(b)))
    }

}
