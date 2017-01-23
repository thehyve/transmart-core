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

class ConceptFullNameTests {

    @Test
    void basicTest() {
        def conceptFullName = new ConceptFullName('\\abc\\d\\e')

        assertThat conceptFullName, hasProperty('length', equalTo(3))
        assert conceptFullName[0] == 'abc'
        assert conceptFullName[1] == 'd'
        assert conceptFullName[2] == 'e'
        assert conceptFullName[3] == null
        assert conceptFullName[-2] == 'd'
    }

    @Test
    void trailingSlashIsOptional() {
        def c1 = new ConceptFullName('\\a'),
            c2 = new ConceptFullName('\\a\\')

        assertThat c1, is(equalTo(c2))
    }

    @Test
    void testBadPaths() {
        def paths = ['', '\\', '\\\\', '\\a\\\\', '\\a\\\\b']

        paths.each({
            try {
                def c = new ConceptFullName(it)
                fail "Could create ConceptFullName with path " + it + ", " +
                        "result: " + c
            } catch (Exception iae) {
                assertThat iae, isA(IllegalArgumentException)
            }
        })
    }
}
