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

import spock.lang.Specification

class VcfDataRowSpec extends Specification {

    public static final double ERROR = 0.001 as Double

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

        expect:
        dataRow.infoFields['DP'] == '88'
        dataRow.infoFields['AF1'] == '1'
        dataRow.infoFields['QD'] == '2'
        dataRow.infoFields['DP4'] == '0,0,80,0'
        dataRow.infoFields['MQ'] == '60'
        dataRow.infoFields['FQ'] == '-268'
        dataRow.infoFields['NOVAL']
    }

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

        expect:
        dataRow.qualityOfDepth - 0.8d < ERROR
    }

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

        expect:
        dataRow.qualityOfDepth - 0.2d < ERROR
    }

    void testQualityOfDepthDotInInfoField() {
        def dataRow = new VcfDataRow(
                quality: 0.9,
                info: "QD=.;AB=1,201;"
        )

        expect: 'QD=.'
        dataRow.qualityOfDepth - 0.9d < ERROR
    }

    void testQualityOfDepthInfoFieldIsNull() {
        def dataRow = new VcfDataRow(
                quality: 0.95,
                info: null
        )

        expect: 'info is null'
        dataRow.qualityOfDepth - 0.95d < ERROR
    }

    void testQualityOfDepthAllNulls() {
        def dataRow = new VcfDataRow(
                quality: null,
                info: null
        )

        expect: 'info and quality is null'
        dataRow.qualityOfDepth == null
    }

}
