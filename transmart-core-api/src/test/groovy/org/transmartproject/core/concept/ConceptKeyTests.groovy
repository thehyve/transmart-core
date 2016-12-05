/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-api.
 *
 * Transmart-core-api is free software: you can redistribute it and/or modify it
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
 * transmart-core-api.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.core.concept

import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.Assert.fail
import static org.hamcrest.Matchers.*

class ConceptKeyTests {

    @Test
    void basicTest() {
        def conceptKey = new ConceptKey('\\\\tableCode\\full\\thing\\')

        assertThat conceptKey, allOf(
                hasProperty('tableCode', equalTo('tableCode')),
                hasProperty('conceptFullName', hasProperty('length',
                        equalTo(2))))
    }

    @Test
    void basicTestAlternativeConstructor() {
        def conceptKey = new ConceptKey('tableCode', '\\full\\thing\\')

        assert conceptKey == new ConceptKey('\\\\tableCode\\full\\thing\\')
    }

    @Test
    void badInput() {
        def keys = ['', '\\\\', '\\\\\\', '\\\\a\\', '\\as\\b',
                '\\\\a\\b\\\\', null]

        keys.each({
            try {
                new ConceptKey(it)
                fail "Could create ConceptKey with value " + it
            } catch (Exception iae) {
                assertThat iae, isA(IllegalArgumentException)
            }
        })
    }

}
