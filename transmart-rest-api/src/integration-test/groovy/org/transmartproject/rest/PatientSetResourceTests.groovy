package org.transmartproject.rest

import grails.web.mime.MimeType

import static org.hamcrest.Matchers.*
import static org.thehyve.commons.test.FastMatchers.mapWith
import static spock.util.matcher.HamcrestSupport.that

class PatientSetResourceTests extends ResourceSpec {


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

    void testSave() {
        when:
        def response = post('/patient_sets') {
            header 'Accept', contentTypeForHAL
            contentType MimeType.XML.name
            xml QUERY_DEFINITION
        }

        then:
        response.status == 201
        that response.json, mapWith(
                setSize: 1,
                status: 'FINISHED',
                id: isA(Number),
                username: 'admin',)
        that response.json, hasSelfLink('/patient_sets/' + response.json['id'])
        that response.json, hasEntry(is('_embedded'),
                hasEntry(is('patients'),
                        contains(allOf(
                                mapWith(
                                        id: -101,
                                        trial: 'STUDY_ID_1',
                                        inTrialId: 'SUBJ_ID_1',),
                                hasSelfLink('/studies/study_id_1/subjects/-101')))))
    }

    void testSaveAndLoad() {
        when:
        def response1 = post('/patient_sets') {
            header 'Accept', contentTypeForHAL
            contentType MimeType.XML.name
            xml QUERY_DEFINITION
        }

        then:
        response1.status == 201

        when:
        def response2 = getAsHal('/patient_sets/' + response1.json['id'])

        then:
        response2.status == 200
        that response2.json, mapWith(
                id: response1.json['id'],
                setSize: 1)
    }

}
