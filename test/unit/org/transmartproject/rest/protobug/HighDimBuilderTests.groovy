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

import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.rest.protobuf.HighDimBuilder
import org.transmartproject.rest.protobuf.HighDimProtos.Row

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.contains
import static org.hamcrest.Matchers.containsInAnyOrder
import static org.hamcrest.Matchers.empty
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.hasProperty
import static org.hamcrest.Matchers.nullValue
import org.transmartproject.core.dataquery.highdim.projections.MultiValueProjection

class HighDimBuilderTests {

    @Test
    void "test single field projection. Double"() {
        def inputDataRow = new TestDataRow(assayColumns: testAssays, data: [1d, 2d])
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
    void "test single field projection. Any number"() {
        def inputDataRow = new TestDataRow(assayColumns: testAssays, data: [1, 2l])
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
        builder.projection = new TestProjection(dataProperties: [a: Integer, b: String])

        def inputDataRow = new TestDataRow(assayColumns: testAssays, data: [[a: 1, b: 'text1'], [a: 2, b: 'text2']])
        Row row = builder.createRow(inputDataRow)

        assertThat row, allOf(
                hasProperty('label', is('test label')),
                hasProperty('bioMarker', is('test biomarker')),
                hasProperty('valueList', containsInAnyOrder(
                        hasProperty('doubleValueList', contains(1d, 2d)),
                        hasProperty('stringValueList', contains('text1', 'text2')))))
    }

    def testAssays = [
            [id: 1, patientInTrialId: 'TRIAL1', platform: [id: 1], sampleCode: 'SAMPLE1'] as AssayColumn,
            [id: 2, patientInTrialId: 'TRIAL2', platform: [id: 1], sampleCode: 'SAMPLE1'] as AssayColumn,
    ]


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

    HighDimBuilder builder
    def out

    @Before
    void before() {
        out = new ByteArrayOutputStream()

        builder = new HighDimBuilder(
                projection: [] as Projection,
                assayColumns: testAssays,
                out: out,
        )
    }

}
