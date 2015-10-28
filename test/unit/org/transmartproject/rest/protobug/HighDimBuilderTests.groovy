/*
 * Copyright 2014 Janssen Research & Development, LLC.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version, along with the following terms:
 *
 *   1. You may convey a work based on this program in accordance with
 *      section 5, provided that you retain the above notices.
 *   2. You may convey verbatim copies of this program code as you receive
 *      it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.rest.protobug

import org.junit.Test
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.core.dataquery.highdim.projections.MultiValueProjection
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.rest.protobuf.HighDimBuilder
import org.transmartproject.rest.protobuf.HighDimProtos.Row

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class HighDimBuilderTests {

    HighDimBuilder builder

    def testAssays = [
            [id: 1, patientInTrialId: 'TRIAL1', platform: [id: 1], sampleCode: 'SAMPLE1'] as AssayColumn,
            [id: 2, patientInTrialId: 'TRIAL2', platform: [id: 1], sampleCode: 'SAMPLE1'] as AssayColumn,
    ]

    private createBuilder(DataRow row, Projection projection = null)  {
        builder = new HighDimBuilder(
                projection ?: ([] as Projection),
                [
                        getIndicesList: { -> testAssays },
                        getRows: {
                            [row].iterator()
                        }
                ] as TabularResult)
    }

    @Test
    void "test single field projection. Double"() {
        def inputDataRow = new TestDataRow(assayColumns: testAssays, data: [1d, 2d])
        createBuilder inputDataRow
        Row row = builder.createRow inputDataRow

        assertThat row, allOf(
                hasProperty('label', is('test label')),
                hasProperty('bioMarker', is('test biomarker')),
                hasProperty('valueList', contains(
                        hasProperty('doubleValueList', contains(1d, 2d))
                ))
        )
    }

    @Test
    void "test single field projection. Any number"() {
        def inputDataRow = new TestDataRow(assayColumns: testAssays, data: [1, 2l])
        createBuilder inputDataRow
        Row row = builder.createRow(inputDataRow)

        assertThat row, allOf(
                hasProperty('label', is('test label')),
                hasProperty('bioMarker', is('test biomarker')),
                hasProperty('valueList', contains(
                        hasProperty('doubleValueList', contains(1d, 2d))
                ))
        )
    }

    @Test
    void "test single field projection. String"() {
        def inputDataRow = new TestDataRow(assayColumns: testAssays, data: ['A', 'B'])
        createBuilder inputDataRow
        Row row = builder.createRow(inputDataRow)

        assertThat row, allOf(
                hasProperty('label', is('test label')),
                hasProperty('bioMarker', is('test biomarker')),
                hasProperty('valueList', contains(
                        hasProperty('stringValueList', contains('A', 'B'))
                ))
        )
    }

    @Test
    void "test multiple field projection."() {
        def projection = new TestProjection(dataProperties: [a: Integer, b: String])
        def inputDataRow = new TestDataRow(assayColumns: testAssays, data: [[a: 1, b: 'text1'], [a: 2, b: 'text2']])
        createBuilder inputDataRow, projection
        Row row = builder.createRow(inputDataRow)

        assertThat row, allOf(
                hasProperty('label', is('test label')),
                hasProperty('bioMarker', is('test biomarker')),
                hasProperty('valueList', containsInAnyOrder(
                        hasProperty('doubleValueList', contains(1d, 2d)),
                        hasProperty('stringValueList', contains('text1', 'text2')))))
    }

    @Test
    void "test column type choice."() {
        assertThat HighDimBuilder.decideColumnValueType(String), sameInstance(String)
        assertThat HighDimBuilder.decideColumnValueType(Object), sameInstance(String)
        assertThat HighDimBuilder.decideColumnValueType(HighDimBuilder), sameInstance(String)

        assertThat HighDimBuilder.decideColumnValueType(double), sameInstance(Double)
        assertThat HighDimBuilder.decideColumnValueType(byte), sameInstance(Double)
        assertThat HighDimBuilder.decideColumnValueType(int), sameInstance(Double)
        assertThat HighDimBuilder.decideColumnValueType(float), sameInstance(Double)
        assertThat HighDimBuilder.decideColumnValueType(Double), sameInstance(Double)
        assertThat HighDimBuilder.decideColumnValueType(Float), sameInstance(Double)
        assertThat HighDimBuilder.decideColumnValueType(BigDecimal), sameInstance(Double)
        assertThat HighDimBuilder.decideColumnValueType(Number), sameInstance(Double)
        assertThat HighDimBuilder.decideColumnValueType(BigInteger), sameInstance(Double)
    }

    class TestProjection implements MultiValueProjection, Projection {
        Map<String, Class> dataProperties

        @Override
        Object doWithResult(Object o) {}
    }

    class TestDataRow implements BioMarkerDataRow {

        List<AssayColumn> assayColumns = []
        List<Object> data = []

        String label = 'test label'

        @Override
        Object getAt(int i) {
            data[i]
        }

        @Override
        Object getAt(Object o) {
            def indx = assayColumns.findIndexOf { o == it }
            data[indx]
        }

        @Override
        Object getAt(AssayColumn assayColumn) {
            getAt(assayColumn)
        }

        @Override
        Iterator<Object> iterator() {
            data.iterator()
        }

        String bioMarker = 'test biomarker'
    }

}
