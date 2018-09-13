/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest.v1

import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.mock.MockUser
import org.transmartproject.rest.MimeTypes
import org.transmartproject.rest.data.V1DefaultTestData

import static org.hamcrest.Matchers.*
import static org.springframework.http.HttpStatus.CREATED
import static org.springframework.http.HttpStatus.OK
import static org.thehyve.commons.test.FastMatchers.mapWith
import static org.transmartproject.rest.utils.HalMatcherUtils.hasSelfLink
import static org.transmartproject.rest.utils.ResponseEntityUtils.toJson
import static spock.util.matcher.HamcrestSupport.that

class PatientSetResourceSpec extends V1ResourceSpec {

    @Autowired
    V1DefaultTestData testData

    public static final String QUERY_DEFINITION = '''
<ns3:query_definition xmlns:ns3="http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/">
  <query_name>My query</query_name>
  <panel>
    <item>
      <item_key>\\\\i2b2 main\\foo\\study1\\bar\\</item_key>
    </item>
  </panel>
</ns3:query_definition>
'''

    void setup() {
        selectUser(new MockUser('test', true))
        testData.clearTestData()
        testData.createTestData()
    }

    void testSave() {
        selectUser(new MockUser('test_user'))

        when:
        def response = post("${contextPath}/patient_sets", MimeTypes.APPLICATION_HAL_JSON, MimeTypes.APPLICATION_XML, QUERY_DEFINITION)

        then:
        response.statusCode == CREATED
        def json = toJson(response)
        that json, mapWith(
                setSize: 1,
                status: 'FINISHED',
                description: 'My query',
                id: isA(Number),
                username: 'test_user',)
        that json, hasSelfLink("${contextPath}/patient_sets/${json['id']}")
        that json, hasEntry(is('_embedded'),
                hasEntry(is('patients'),
                        contains(allOf(
                                mapWith(
                                        id: -101,
                                        trial: 'STUDY_ID_1',
                                        inTrialId: 'SUBJ_ID_1',),
                                hasSelfLink("${contextPath}/studies/study_id_1/subjects/-101")))))
    }

    void testSaveAndLoad() {
        when:
        def response1 = post("${contextPath}/patient_sets", MimeTypes.APPLICATION_HAL_JSON, MimeTypes.APPLICATION_XML, QUERY_DEFINITION)

        then:
        response1.statusCode == CREATED

        when:
        def json1 = toJson(response1)
        def response2 = get "${contextPath}/patient_sets/" + json1['id'], MimeTypes.APPLICATION_HAL_JSON

        then:
        response2.statusCode == OK
        that toJson(response2), mapWith(
                id: json1['id'],
                setSize: 1)
    }

}
