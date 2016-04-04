/*
 * Copyright (c) 2016 The Hyve B.V.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This code is licensed under the GNU General Public License,
 * version 3, or (at your option) any later version.
 */

package org.transmartproject.rest

import org.transmartproject.db.dataquery.highdim.SampleBioMarkerTestData

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class BioMarkerResourceTests extends ResourceTestCase {

    SampleBioMarkerTestData bioMarkerTestData = new SampleBioMarkerTestData()

    def getTypesMatcher() {
        containsInAnyOrder(*bioMarkerTestData.allBioMarkers*.type.unique().collect {
            hasEntry('type', it)
        })
    }
    def getTypesMatcherHal() {
        containsInAnyOrder(*bioMarkerTestData.allBioMarkers*.type.unique().collect {
            allOf(hasSelfLink("/biomarkers/${it}"), hasEntry('type', it))
        })
    }
    def getOrganismsMatcher() {
        containsInAnyOrder(*bioMarkerTestData.allBioMarkers*.organism.unique().collect {
            hasEntry('organism', it)
        })
    }

    def getGeneMarkersMatcher() {
        containsInAnyOrder(*bioMarkerTestData.geneBioMarkers.collect {
            allOf(
                    hasEntry('description', it.description),
                    hasEntry('name', it.name),
                    hasEntry('organism', it.organism),
                    hasEntry('externalId', it.externalId),
                    hasEntry('sourceCode', it.sourceCode),
                    hasEntry('type', it.type)
            )
        })
    }

    void testBioMarkerTypes() {
        get("${baseURL}biomarkers")
        assertStatus 200

        assertThat JSON, allOf(
                hasEntry(is('types'), typesMatcher),
                hasEntry(is('organisms'), organismsMatcher)
        )
    }

    void testBioMarkerTypesAsHal() {
        def result = getAsHal("${baseURL}biomarkers")

        assertStatus 200

        assertThat result, halIndexResponse('/biomarkers', [
                //foo: containsInAnyOrder(hasEntry('bar', 'baz')),
                types: typesMatcherHal,
                organisms: organismsMatcher
        ])
    }

    void testGetBioMarker() {
        def type = 'GENE'
        get("${baseURL}biomarkers/${type}")
        assertStatus 200

        assertThat JSON, hasEntry(is('biomarkers'), geneMarkersMatcher)
    }

    void testGetBioMarkerAsHal() {
        def type = 'GENE'
        def result = getAsHal("${baseURL}biomarkers/${type}")
        assertStatus 200

        //log.info "testGetStudyAsHal:\n" + result.toString(2)

        assertThat result, halIndexResponse("/biomarkers/${type}", [
                biomarkers: geneMarkersMatcher
        ])
    }

    void testGetNonExistentStudy() {
        def type = 'NONEXISTANT_TYPE'
        get("${baseURL}biomarkers/${type}")
        assertStatus 200

        assertThat JSON, hasEntry(is('biomarkers'), empty())
    }

}
