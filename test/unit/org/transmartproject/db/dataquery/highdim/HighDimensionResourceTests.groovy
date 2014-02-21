package org.transmartproject.db.dataquery.highdim

import org.gmock.WithGMock
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsInAnyOrder

@WithGMock
class HighDimensionResourceTests {

    /*
     * Calling this HighDimensionResourceServiceTests auto-magically
     * makes this @TesFor(HighDimensionResourceService), which won't do.
     * HighDimensionResourceService has an autowired dependency that cannot
     * be satisfied (StandardAssayConstraintFactory). I first tried to
     * annotate this simply with @TestMixin(ServiceUnitTestMixin) and define
     * this dependent bean in setUp() with defineBeans {}, but that won't do
     * as well because StandardAssayConstraintFactory has, in its turn, other
     * dependencies that cannot be satisfied. At this point, I would have to
     * define those beans as well, which would mean this test would depend
     * on implementation details of collaborators of
     * HighDimensionResourceService, which is not an acceptable situation.
     *
     * So I renamed the test, removed @TestMixin and created the testee
     * manually.
     */

    HighDimensionResourceService testee = new HighDimensionResourceService()

    @Test
    void testKnownTypes() {
        def dataTypes = [ 'datatype_1', 'datatype_2' ]
        dataTypes.each {
            testee.registerHighDimensionDataTypeModule(it, mock(Closure))
        }

        assertThat testee.getKnownTypes(), containsInAnyOrder(*dataTypes)
    }
}
