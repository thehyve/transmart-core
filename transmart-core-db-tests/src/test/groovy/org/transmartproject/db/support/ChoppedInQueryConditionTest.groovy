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

package org.transmartproject.db.support

import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class ChoppedInQueryConditionTest {

    @Test
    void testNumOfItemsLessThenMax() {
        def condition = new ChoppedInQueryCondition('patient_num', [1, 2, 3])
        assertThat condition.parametersValues, hasEntry(is('_0'), contains(1, 2, 3))
        assertThat condition.queryConditionTemplate, equalTo("(patient_num IN (:_0))")
    }

    @Test
    void testNumOfItemsMoreThenMax() {
        def condition = new ChoppedInQueryCondition('patient_num', (1..2500).toList())
        assertThat condition.parametersValues, allOf(
                hasEntry(is('_0'), hasSize(1000)),
                hasEntry(is('_1'), hasSize(1000)),
                hasEntry(is('_2'), hasSize(500)),
        )
        assertThat condition.queryConditionTemplate, equalTo("(patient_num IN (:_0) OR patient_num IN (:_1) OR patient_num IN (:_2))")
        assertThat condition.populatedQueryCondition, allOf(
                startsWith("(patient_num IN ('1','2','3',"),
                containsString("'1000') OR patient_num IN ('1001','1002','1003',"),
                containsString("'2000') OR patient_num IN ('2001','2002','2003',"), endsWith(",'2498','2499','2500'))")
        )
    }

    @Test
    void tesEmptyIds() {
        def condition = new ChoppedInQueryCondition('patient_num', [])
        assertThat condition.parametersValues, equalTo([:])
        assertThat condition.queryConditionTemplate, equalTo("(0=1)")
    }

}