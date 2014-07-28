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

package org.transmartproject.db.dataquery.highdim

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.gmock.WithGMock
import org.hibernate.ScrollableResults
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@TestMixin(GrailsUnitTestMixin)
@WithGMock
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

    @Test
    void testEmptyResultSet() {
        ScrollableResults mockedResults = mock(ScrollableResults)
        def testee = new DefaultHighDimensionTabularResult(
                results:      mockedResults,
                closeSession: false)

        mockedResults.next().returns(false)
        mockedResults.get().returns(null)

        play {
            shouldFail NoSuchElementException, {
                testee.iterator().next()
            }
        }

        testee.close()
    }

}
