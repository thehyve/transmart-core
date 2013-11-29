package org.transmartproject.db.dataquery.highdim

import grails.test.mixin.TestFor
import org.gmock.WithGMock
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsInAnyOrder

@TestFor(HighDimensionResourceService)
@WithGMock
class HighDimensionResourceServiceTests {

    @Test
    void testKnownTypes() {
        def dataTypes = [ 'datatype_1', 'datatype_2' ]
        dataTypes.each {
            service.registerHighDimensionDataTypeModule(it, mock(Closure))
        }

        assertThat service.getKnownTypes(), containsInAnyOrder(*dataTypes)
    }
}
