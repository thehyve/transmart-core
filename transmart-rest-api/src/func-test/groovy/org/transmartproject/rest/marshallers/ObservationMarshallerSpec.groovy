/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest.marshallers

import groovy.json.JsonSlurper
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity

import static org.hamcrest.Matchers.*
import static org.thehyve.commons.test.FastMatchers.mapWith
import static ResourceSpec.hasLinks
import static spock.util.matcher.HamcrestSupport.that

class ObservationMarshallerSpec extends MarshallerSpec {

    public static final String VERSION = "v1"
    def patients = -101
    def concept_paths = '\\foo\\study1\\bar\\'

    void basicTest() {
        given:
        testDataSetup()

        when:
        def url = "${baseURL}/$VERSION/observations?patients=${patients}&concept_paths=${concept_paths}".toString()
        ResponseEntity<Resource> response = getJson(url)
        String content = response.body.inputStream.readLines().join('\n')
        def result = new JsonSlurper().parseText(content)

        then:
        response.statusCode.value() == 200
        response.headers.getFirst('Content-Type').split(';')[0]  == 'application/json'
        result != null
        (result as List).any { Map observation ->
            observation.subject && observation.subject.id == -101
        }
    }

    void testHal() {
        given:
        testDataSetup()

        when:
        def url = "${baseURL}/$VERSION/observations?patients=${patients}&concept_paths=${concept_paths}".toString()
        ResponseEntity<Resource> response = getHal(url)
        String content = response.body.inputStream.readLines().join('\n')
        def result = new JsonSlurper().parseText(content)

        then:
        response.statusCode.value() == 200
        response.headers.getFirst('Content-Type').split(';')[0] == 'application/hal+json'
        that result as Map, allOf(
                hasLinks([:]),
                hasEntry(
                        is('_embedded'),
                        hasEntry(
                                is('observations'),
                                contains(
                                        mapWith(
                                                label: '\\foo\\study1\\bar\\',
                                                value: 10.00000,
                                        )))))
    }

}
