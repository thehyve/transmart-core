/*
 * Copyright © 2017 The Hyve B.V.
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

package org.transmartproject.db.support
import spock.lang.Specification

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class InQueryTest extends Specification {

    void testNumOfItemsLessThenMax() {
        when:
        def choppedList = InQuery.chopParametersValues([1, 2, 3])
        then:
        assertThat choppedList, hasSize(1)
        assertThat choppedList[0], contains(1, 2, 3)
    }

    void testNumOfItemsMoreThenMax() {
        when:
        def choppedList = InQuery.chopParametersValues((1..2500).toList())
        then:
        assertThat choppedList, hasSize(3)
        assertThat choppedList[0], hasSize(1000)
        assertThat choppedList[1], hasSize(1000)
        assertThat choppedList[2], hasSize(500)

    }

    void testEmptyIds() {
        when:
        def choppedList = InQuery.chopParametersValues([[]])
        then:
        assertThat choppedList[0][0], hasSize(0)
    }
}