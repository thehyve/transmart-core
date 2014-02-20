package org.transmartproject.rest

import org.hamcrest.Matchers
import org.junit.Ignore

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class SubjectsResourceTests extends ApiResourceTests {

    def study = 'STUDY1'
    //TODO Assert more subjects fields
    void testListAllSubjectsForStudy() {
        get("${baseUrl}studies/${study}/subjects")
        assertStatus 200

        assertThat JSON, containsInAnyOrder(
                allOf(
                        hasEntry('id',            -101),
                        hasEntry('trial',         'STUDY1'),
                        hasEntry('inTrialId',     'SUBJ_ID_1'),
                        hasEntry('sex',           'UNKOWN'),
                ),
                allOf(
                        hasEntry('id',            -102),
                        hasEntry('trial',         'STUDY1'),
                        hasEntry('inTrialId',     'SUBJ_ID_2'),
                        hasEntry('sex',           'UNKOWN'),
                ),
                allOf(
                        hasEntry('id',            -103),
                        hasEntry('trial',         'STUDY1'),
                        hasEntry('inTrialId',     'SUBJ_ID_3'),
                        hasEntry('sex',           'UNKOWN'),
                )
        )
    }

    void testGetSubjectForStudy() {
        int patientNum = -101
        get("${baseUrl}studies/${study}/subjects/${patientNum}")
        assertStatus 200
        assertThat JSON, allOf(
                hasEntry('id',            patientNum),
                hasEntry('trial',         'STUDY1'),
                hasEntry('inTrialId',     'SUBJ_ID_1'),
                hasEntry('sex',           'UNKOWN'),
        )
    }

    /*//FIXME response contains null
        void testGetNonExistentStudy() {
            def studyName = 'STUDY_NOT_EXIST'
            get("${baseUrl}studies/${studyName}/subjects")
            assertStatus 404

            assertThat JSON, allOf(
                    hasEntry('httpStatus', 404),
                    hasEntry('type', 'NoSuchResourceException'),
                    hasEntry('message', "No study with name '${studyName}' was found"),
            )
        }

                //FIXME response contains null
        void testGetNonExistentSubjectForStudy() {
            def patientNum = -9999
            get("${baseUrl}studies/${study}/subjects/${patientNum}")
            assertStatus 404

            assertThat JSON, allOf(
                    hasEntry('httpStatus', 404),
                    hasEntry('type', 'NoSuchResourceException'),
                    hasEntry('message', "No subject with id '${studyName}' was found"),
            )
        }*/
}

