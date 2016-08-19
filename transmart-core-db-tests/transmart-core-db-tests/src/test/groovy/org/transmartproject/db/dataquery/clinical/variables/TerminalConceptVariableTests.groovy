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

package org.transmartproject.db.dataquery.clinical.variables

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@TestMixin(GrailsUnitTestMixin)
class TerminalConceptVariableTests {

    @Test
    void testEquals() {
        def empty = new TerminalConceptVariable()
        def empty2 = new TerminalConceptVariable()
        def onlyConceptPath = new TerminalConceptVariable(conceptPath: '\\foo\\bar\\')
        def onlyConceptPath2 = new TerminalConceptVariable(conceptPath: '\\foo\\bar\\')
        def onlyConceptCode = new TerminalConceptVariable(conceptCode: 'foobar')
        def onlyConceptCode2 = new TerminalConceptVariable(conceptCode: 'foobar')
        def conceptPathAndCode = new TerminalConceptVariable(conceptPath: '\\foo\\bar\\', conceptCode: 'foobar')
        def conceptPathAndCode2 = new TerminalConceptVariable(conceptPath: '\\foo\\bar\\', conceptCode: 'foobar')

        assertThat empty, is(equalTo(empty2))
        assertThat onlyConceptPath, is(equalTo(onlyConceptPath2))
        assertThat onlyConceptCode, is(equalTo(onlyConceptCode2))
        assertThat conceptPathAndCode2, is(equalTo(conceptPathAndCode2))

        // maybe it would make more sense to return true if tje concept path
        // OR the concept key are equal
        assertThat onlyConceptPath, is(not(equalTo(conceptPathAndCode)))
        assertThat onlyConceptCode, is(not(equalTo(conceptPathAndCode)))
    }

}
