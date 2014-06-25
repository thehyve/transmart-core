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

package org.transmartproject.rest

import org.hamcrest.Matcher
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.dataquery.highdim.acgh.AcghValuesProjection
import org.transmartproject.rest.protobuf.HighDimBuilder
import org.transmartproject.rest.protobuf.HighDimProtos

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsInAnyOrder
import static org.thehyve.commons.test.FastMatchers.*

class HighDimResourceTests extends ResourceTestCase {

    def expectedMrnaAssays = [-402, -401]*.toLong() //groovy autoconverts to BigInteger, and we have to force Long here
    def expectedMrnaRowLabels = ["1553513_at", "1553510_s_at","1553506_at"]

    def expectedAcghAssays = [-3001, -3002]*.toLong()
    def expectedAcghRowLabels = ["cytoband1", "cytoband2"]

    def mrnaSupportedProjections = ['default_real_projection', 'zscore', 'log_intensity', 'all_data']

    Map<String,String> indexUrlMap = [
            mrna: "/studies/study_id_1/concepts/bar/highdim",
            acgh: "/studies/study_id_2/concepts/study1/highdim",
    ]

    void testSummaryAsJson() {
        def result = getAsJson indexUrlMap['mrna']
        assertStatus 200

        Map summary = [
                dataTypes: [getExpectedMrnaSummary()]
        ]

        assertThat result, mapWith(summary)
    }

    void testSummaryAsHal() {
        String url = indexUrlMap['mrna']
        Map result = getAsHal url
        assertStatus 200

        Map summary = getExpectedMrnaSummary()
        summary['_links'] = getExpectedMrnaHalLinks()

        Map expected = [
                '_links': [
                        self: [href: url]
                ],
                '_embedded': [
                        dataTypes: [summary]
                ]
        ]

        assertThat result, mapWith(expected)
    }

    private Map getExpectedMrnaSummary() {
        [
                assayCount: 2,
                name: 'mrna',
                supportedProjections: mrnaSupportedProjections,
                genomeBuildId: 'hg19'
        ]
    }

    Map getExpectedMrnaHalLinks() {
        String selfDataLink = getHighDimUrl('mrna')
        Map expectedLinks = mrnaSupportedProjections.collectEntries { [(it):("${selfDataLink}&projection=${it}")] }
        expectedLinks.put('self', selfDataLink)

        expectedLinks.collectEntries {
            String tempUrl = "${it.value}"
            [(it.key): ([href: tempUrl])]
        }
    }

    void testMrna() {
        HighDimResult result = getAsHighDim(getHighDimUrl('mrna'))
        assertResult(result, expectedMrnaAssays, expectedMrnaRowLabels, ['value': Double])
    }

    void testMrnaDefaultRealProjection() {
        HighDimResult result = getAsHighDim(getHighDimUrl('mrna', Projection.DEFAULT_REAL_PROJECTION))
        assertResult(result, expectedMrnaAssays, expectedMrnaRowLabels, ['value': Double])
    }

    void testMrnaAllDataProjection() {
        HighDimResult result = getAsHighDim(getHighDimUrl('mrna', Projection.ALL_DATA_PROJECTION))
        Map<String, Class> dataColumns = [
                trialName: String,
                rawIntensity: Double,
                logIntensity: Double,
                zscore: Double
        ]
        assertResult(result, expectedMrnaAssays, expectedMrnaRowLabels, dataColumns)
    }

    void testAcgh() {
        HighDimResult result = getAsHighDim(getHighDimUrl('acgh'))
        Map<String, Class> dataColumns = new AcghValuesProjection().dataProperties
        assertResult(result, expectedAcghAssays, expectedAcghRowLabels, dataColumns)
    }

    private String getHighDimUrl(String dataType) {
        "${indexUrlMap[dataType]}?dataType=${dataType}"
    }

    private String getHighDimUrl(String dataType, String projection) {
        "${indexUrlMap[dataType]}?dataType=${dataType}&projection=${projection}"
    }

    /**
     * Verified the given HighDimResult is correct in terms of rows (labels) and columns (assayIds and mapColumns).
     *
     * @param result actual result to assert
     * @param expectedAssays expected assay Ids
     * @param expectedRowLabels expected row labels
     * @param columnsMap expected column names and type
     */
    private assertResult(HighDimResult result,
                         List<Long> expectedAssays,
                         List<String> expectedRowLabels,
                         Map<String, Class> columnsMap) {

        assertThat result.header.assayList*.assayId, containsInAnyOrder(expectedAssays.toArray())

        assertThat result.header.columnSpecList, columnSpecMatcher(columnsMap)

        assertThat result.rows*.label, containsInAnyOrder(expectedRowLabels.toArray())
    }

    private Matcher columnSpecMatcher(Map<String, Class> dataProperties) {

        listOf(dataProperties.collect {
            propsWith([
                    name: it.key,
                    type: HighDimBuilder.typeForClass(it.value)
            ])
        })
    }

    private HighDimResult getAsHighDim(String url) {
        InputStream is = getAsInputStream(url)
        HighDimResult result = new HighDimResult()

        try {
            result.header = HighDimProtos.HighDimHeader.parseDelimitedFrom(is)
            HighDimProtos.Row row
            while ((row = HighDimProtos.Row.parseDelimitedFrom(is)) != null) {
                result.rows << row
            }
        } finally {
            is.close()
        }

        result
    }

    class HighDimResult {
        HighDimProtos.HighDimHeader header
        List<HighDimProtos.Row> rows = []
    }

}
