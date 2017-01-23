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

package org.transmartproject.db.dataquery.highdim.vcf

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@TestMixin(GrailsUnitTestMixin)
class VcfDataRowTests {

    public static final double ERROR = 0.001 as Double

    @Test
    void testAdditionalInfo() {
        VcfDataRow dataRow = new VcfDataRow(
                // Chromosome to define the position
                chromosome: 1,
                position: 500,
                rsId: "rs0001",

                // Reference and alternatives for this position
                referenceAllele: "G",
                alternatives: "A,T,CT",

                // Study level properties are irrelevant for the cohort statistics
                quality: 1.0,
                filter: "",
                info: "DP=88;AF1=1;QD=2;DP4=0,0,80,0;MQ=60;FQ=-268;NOVAL",
                format: "",
                variants: "",

                data: []
        )

        assertThat dataRow.infoFields, allOf(
                hasEntry(equalTo('DP'), equalTo('88')),
                hasEntry(equalTo('AF1'), equalTo('1')),
                hasEntry(equalTo('QD'), equalTo('2')),
                hasEntry(equalTo('DP4'), equalTo('0,0,80,0')),
                hasEntry(equalTo('MQ'), equalTo('60')),
                hasEntry(equalTo('FQ'), equalTo('-268')),

                hasEntry(equalTo("NOVAL"), equalTo(true))
        )
    }

    @Test
    void testNoQualityOfDepthInInfoField() {
        def dataRow = new VcfDataRow(
                // Chromosome to define the position
                chromosome: 1,
                position: 500,
                rsId: "rs0001",

                // Reference and alternatives for this position
                referenceAllele: "G",
                alternatives: "A,T,CT",

                // Study level properties are irrelevant for the cohort statistics
                quality: 0.8,
                filter: "",
                info: "DP=1.0;AB=1,201;TS=.;NOVAL",
                format: "",
                variants: "",

                data: []
        )

        assertThat dataRow.qualityOfDepth, closeTo(0.8 as Double, ERROR)
    }

    @Test
    void testQualityOfDepthInInfoField() {
        def dataRow = new VcfDataRow(
                // Chromosome to define the position
                chromosome: 1,
                position: 500,
                rsId: "rs0001",

                // Reference and alternatives for this position
                referenceAllele: "G",
                alternatives: "A,T,CT",

                // Study level properties are irrelevant for the cohort statistics
                quality: 1.0,
                filter: "",
                info: "DP=1.0;AB=1,201;TS=.;NOVAL;QD=0.2",
                format: "",
                variants: "",

                data: []
        )

        assertThat dataRow.qualityOfDepth, closeTo(0.2 as Double, ERROR)
    }

    @Test
    void testQualityOfDepthDotInInfoField() {
        def dataRow = new VcfDataRow(
                quality: 0.9,
                info: "QD=.;AB=1,201;"
        )

        assertThat 'QD=.', dataRow.qualityOfDepth, closeTo(0.9 as Double, ERROR)
    }

    @Test
    void testQualityOfDepthInfoFieldIsNull() {
        def dataRow = new VcfDataRow(
                quality: 0.95,
                info: null
        )

        assertThat 'info is null', dataRow.qualityOfDepth, closeTo(0.95 as Double, ERROR)
    }

    @Test
    void testQualityOfDepthAllNulls() {
        def dataRow = new VcfDataRow(
                quality: null,
                info: null
        )

        assertNull 'info and quality is null', dataRow.qualityOfDepth
    }

}
