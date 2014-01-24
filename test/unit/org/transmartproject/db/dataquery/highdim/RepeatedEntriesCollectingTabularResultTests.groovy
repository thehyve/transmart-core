package org.transmartproject.db.dataquery.highdim

import com.google.common.collect.Lists
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.TabularResult

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@TestMixin(GrailsUnitTestMixin)
class RepeatedEntriesCollectingTabularResultTests {

    def tabularResult

    @Before
    void setUp() {
        tabularResult = new TabularResult() {

            @Override
            List getIndicesList() { [] }

            @Override
            Iterator getRows() {
                [
                        [getLabel: { 'A' }] as AbstractDataRow,
                        [getLabel: { 'A' }] as AbstractDataRow,
                        [getLabel: { 'B' }] as AbstractDataRow,
                        [getLabel: { 'B' }] as AbstractDataRow,
                        [getLabel: { 'A' }] as AbstractDataRow,
                ].iterator()
            }

            @Override
            String getColumnsDimensionLabel() { 'Column dimension' }

            @Override
            String getRowsDimensionLabel() { 'Rows dimension' }

            @Override
            void close() throws IOException {}

            @Override
            Iterator iterator() { getRows() }
        }
    }

    @Test
    void testCollect() {
        def result = new RepeatedEntriesCollectingTabularResult(
                tabularResult: tabularResult,
                collectBy: { it.label },
                resultItem: { collection ->
                    [ getLabel: collection*.label.join('|') ] as AbstractDataRow
                }
        )

        def resultList = Lists.newArrayList result

        assertThat resultList, contains(
                hasProperty('label', equalTo('A|A')),
                hasProperty('label', equalTo('B|B')),
                hasProperty('label', equalTo('A')),
        )
    }

}
