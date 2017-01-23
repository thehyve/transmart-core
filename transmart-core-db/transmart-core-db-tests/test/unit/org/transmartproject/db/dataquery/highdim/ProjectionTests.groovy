package org.transmartproject.db.dataquery.highdim
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.projections.Projection

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsInAnyOrder
import static org.hamcrest.Matchers.equalTo

/**
 * Created by dverbeec on 03/08/2016.
 */

@TestMixin(GrailsUnitTestMixin)
class ProjectionTests {

    @Test
    void testAllProjectionsHavePrettyNames() {
        // get all statically defined strings
        def projections = Projection.declaredFields.grep {!it.synthetic && it.name != 'prettyNames'}*.get()

        // assert that all statically defined strings have a pretty name in the prettyNames dictionary
        assertThat Projection.prettyNames.keySet(), containsInAnyOrder(projections.collect {equalTo(it)})
    }
}
