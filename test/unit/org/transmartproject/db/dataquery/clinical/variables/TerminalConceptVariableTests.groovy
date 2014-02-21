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
