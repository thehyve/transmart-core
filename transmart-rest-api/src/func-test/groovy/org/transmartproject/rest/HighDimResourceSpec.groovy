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
import org.transmartproject.rest.matchers.HighDimResult
import org.transmartproject.rest.protobuf.HighDimBuilder
import org.transmartproject.rest.protobuf.HighDimProtos
import spock.lang.Ignore

import static org.hamcrest.Matchers.*
import static org.thehyve.commons.test.FastMatchers.*
import static spock.util.matcher.HamcrestSupport.that

class HighDimResourceSpec extends ResourceSpec {

    public static final String VERSION = "v1"
    def expectedMrnaAssays = [-402, -401]*.toLong() //groovy autoconverts to BigInteger, and we have to force Long here
    def expectedMrnaRowLabels = ["1553513_at", "1553510_s_at", "1553506_at"]

    def expectedAcghAssays = [-3001, -3002]*.toLong()
    def expectedAcghRowLabels = ['region 1:33-9999', 'region 2:66-99']

    def mrnaSupportedProjections = ['default_real_projection', 'zscore', 'log_intensity', 'all_data']
    def mrnaSupportedAssayConstraints = ['patient_set', 'disjunction',
                                         'assay_id_list', 'ontology_term',
                                         'trial_name',]
    def mrnaSupportedDataConstraints = ['gene_lists', 'search_keyword_ids',
                                        'gene_signatures', 'genes',
                                        'disjunction', 'pathways', 'proteins',]

    Map<String, String> indexUrlMap = [
            mrna: "/$VERSION/studies/study_id_1/concepts/bar/highdim",
            acgh: "/$VERSION/studies/study_id_2/concepts/study1/highdim",
    ]

    void testSummaryAsJson() {
        when:
        def response = get indexUrlMap['mrna'], {
            header 'Accept', contentTypeForJSON
        }

        then:
        response.status == 200
        that response.json as Map, mapWith([
                dataTypes: [getExpectedMrnaSummary()]
        ])
    }

    void testSummaryAsHal() {
        String url = indexUrlMap['mrna']
        Map summary = getExpectedMrnaSummary()
        summary['_links'] = getExpectedMrnaHalLinks()

        Map expected = [
                '_links'   : [
                        self: [href: url]
                ],
                '_embedded': [
                        dataTypes: [summary]
                ]
        ]

        when:
        def response = get url, {
            header 'Accept', contentTypeForHAL
        }

        then:
        response.status == 200
        that response.json as Map, mapWith(expected)
    }

    void testMrna() {
        when:
        HighDimResult result = getAsHighDim(getHighDimUrl('mrna'))

        then:
        that result.header.assayList*.assayId, containsInAnyOrder(expectedMrnaAssays.toArray())
        that result.header.columnSpecList, columnSpecMatcher(['value': Double])
        that result.rows*.label, containsInAnyOrder(expectedMrnaRowLabels.toArray())
    }

    void testMrnaDefaultRealProjection() {
        when:
        HighDimResult result = getAsHighDim(getHighDimUrl('mrna', Projection.DEFAULT_REAL_PROJECTION))

        then:
        that result.header.assayList*.assayId, containsInAnyOrder(expectedMrnaAssays.toArray())
        that result.header.columnSpecList, columnSpecMatcher(['value': Double])
        that result.rows*.label, containsInAnyOrder(expectedMrnaRowLabels.toArray())
    }

    void testMrnaAllDataProjection() {
        Map<String, Class> dataColumns = [
                trialName   : String,
                rawIntensity: Double,
                logIntensity: Double,
                zscore      : Double
        ]

        when:
        HighDimResult result = getAsHighDim(getHighDimUrl('mrna', Projection.ALL_DATA_PROJECTION))

        then:
        that result.header.assayList*.assayId, containsInAnyOrder(expectedMrnaAssays.toArray())
        that result.header.columnSpecList, columnSpecMatcher(dataColumns)
        that result.rows*.label, containsInAnyOrder(expectedMrnaRowLabels.toArray())
    }

    @Ignore
    void testAcgh() {
        Map<String, Class> dataColumns = new AcghValuesProjection().dataProperties

        when:
        HighDimResult result = getAsHighDim(getHighDimUrl('acgh', 'acgh_values'))

        then:
        that result.header.assayList*.assayId, containsInAnyOrder(expectedAcghAssays.toArray())
        that result.header.columnSpecList, columnSpecMatcher(dataColumns)
        that result.rows*.label, containsInAnyOrder(expectedAcghRowLabels.toArray())
    }

    void testAssayConstraints() {
        def assayConstraints = '{assay_id_list: { ids: [-3002] }}'

        when:
        HighDimResult result = getAsHighDim(
                getHighDimUrl('acgh', null, assayConstraints))
        then:
        that result.header.assayList, contains(
                hasProperty('assayId', equalTo(-3002L))
        )
    }

    void testMultipleAssayConstraintsOfTheSameType() {
        def assayConstraints =
                '{ assay_id_list: [ { ids: [-3001, -3002] }, { ids: [-3002] } ] }'

        when:
        HighDimResult result = getAsHighDim(
                getHighDimUrl('acgh', null, assayConstraints))

        then:
        that result.header.assayList, contains(
                hasProperty('assayId', equalTo(-3002L))
        )
    }

    void testDataConstraint() {
        def dataConstraints = '{ genes: [ { names: ["ADIRF"] } ] }'

        when:
        HighDimResult result = getAsHighDim(
                getHighDimUrl('acgh', null, null, dataConstraints))

        then:
        that result.rows, contains(
                hasProperty('bioMarker', equalTo('ADIRF'))
        )
    }

    private String getHighDimUrl(String dataType,
                                 String projection = null,
                                 String assayConstraints = null,
                                 String dataConstraints = null) {
        def ret = "${indexUrlMap[dataType]}?dataType=${dataType}"
        if (projection) {
            ret += "&projection=${projection}"
        }
        if (assayConstraints) {
            ret += '&assayConstraints='
            ret += URLEncoder.encode(assayConstraints, 'UTF-8')
        }
        if (dataConstraints) {
            ret += '&dataConstraints='
            ret += URLEncoder.encode(dataConstraints, 'UTF-8')
        }
        ret
    }

    private Map getExpectedMrnaSummary() {
        [
                assayCount               : 2,
                name                     : 'mrna',
                supportedProjections     : hasItems(*mrnaSupportedProjections),
                supportedAssayConstraints: hasItems(*mrnaSupportedAssayConstraints),
                supportedDataConstraints : hasItems(*mrnaSupportedDataConstraints),
                genomeBuildId            : 'hg19'
        ]
    }

    private Map getExpectedMrnaHalLinks() {
        String selfDataLink = getHighDimUrl('mrna')
        Map expectedLinks = mrnaSupportedProjections.collectEntries { [(it): ("${selfDataLink}&projection=${it}")] }
        expectedLinks.put('self', selfDataLink)

        expectedLinks.collectEntries {
            String tempUrl = "${it.value}"
            [(it.key): ([href: tempUrl])]
        }
    }

    private static Matcher columnSpecMatcher(Map<String, Class> dataProperties) {

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

}
