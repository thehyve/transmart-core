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

package org.transmartproject.rest.marshallers

import groovy.json.JsonSlurper
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.transmartproject.rest.TestData

import java.text.DateFormat
import java.text.SimpleDateFormat

import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that

class PatientMarshallerSpec extends MarshallerSpec {

    public static final String VERSION = "v1"

    void basicTest() {
        when:
        def url = "${baseURL}/${VERSION}/studies/${TestData.TRIAL}/subjects/${TestData.ID}"
        ResponseEntity<Resource> response = getJson(url)
        String content = response.body.inputStream.readLines().join('\n')
        def result = new JsonSlurper().parseText(content)

        then:
        response.statusCode.value() == 200
        response.headers.getFirst('Content-Type').split(';')[0] == 'application/json'
        that result as Map, allOf(
                hasEntry('id', TestData.ID as Integer),
                hasEntry('trial', TestData.TRIAL),
                hasEntry('inTrialId', TestData.SUBJECT_ID),
                hasEntry('birthDate', formatAsISO(TestData.BIRTH_DATE)),
                hasEntry('sex', TestData.SEX.name()),
                hasEntry(is('deathDate'), is(nullValue())),
                hasEntry('age', TestData.AGE as Integer),
                hasEntry('race', TestData.RACE),
                hasEntry('maritalStatus', TestData.MARITAL_STATUS),
                hasEntry('religion', TestData.RELIGION))
    }

    private static String formatAsISO(Date date) {
        TimeZone tz = TimeZone.getTimeZone 'UTC'
        DateFormat df = new SimpleDateFormat('yyyy-MM-dd\'T\'HH:mm:ss\'Z\'')
        df.timeZone = tz
        df.format date
    }

    void testHal() {
        when:
        def url = "${baseURL}/${VERSION}/studies/${TestData.TRIAL}/subjects/${TestData.ID}"
        ResponseEntity<Resource> response = getHal(url)
        String content = response.body.inputStream.readLines().join('\n')
        def result = new JsonSlurper().parseText(content)

        then:
        response.statusCode.value() == 200
        response.headers.getFirst('Content-Type').split(';')[0] == 'application/hal+json'
        that result as Map, allOf(
                hasEntry('age', TestData.AGE as Integer),
                hasEntry('race', TestData.RACE),
                hasEntry('maritalStatus', TestData.MARITAL_STATUS),
                // do not test the rest
                hasEntry(is('_links'),
                        hasEntry(is('self'),
                                hasEntry(is('href'), is("/${VERSION}/studies/${TestData.TRIAL_LC}/subjects/${TestData.ID}".toString()))
                        )
                )
        )
    }

}
