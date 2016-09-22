package org.transmartproject.rest

import grails.web.mime.MimeType

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.thehyve.commons.test.FastMatchers.mapWith

class PatientSetResourceTests extends ResourceSpecification {


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
        def resp = postAsHal('/patient_sets') {
            contentType MimeType.XML.name
            body {
                QUERY_DEFINITION
            }
        }

        assertStatus 201

        assertThat resp, mapWith(
                setSize: 1,
                status: 'FINISHED',
                id: isA(Number),
                username: 'admin',)

        assertThat resp, hasSelfLink('/patient_sets/' + resp['id'])

        assertThat resp, hasEntry(is('_embedded'),
                hasEntry(is('patients'),
                        contains(allOf(
                                mapWith(
                                        id: -101,
                                        trial: 'STUDY_ID_1',
                                        inTrialId: 'SUBJ_ID_1',),
                                hasSelfLink('/studies/study_id_1/subjects/-101')))))
    }

    void testSaveAndLoad() {
        def resp = postAsHal('/patient_sets') {
            contentType MimeType.XML.name
            body {
                QUERY_DEFINITION
            }
        }

        assertStatus 201
        def id = resp['id']
        resp = getAsHal('/patient_sets/' + id)

        assertStatus 200

        assertThat resp, mapWith(
                id: id,
                setSize: 1)
    }

}
