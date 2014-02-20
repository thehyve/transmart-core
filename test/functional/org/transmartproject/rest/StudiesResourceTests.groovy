package org.transmartproject.rest

import com.grailsrocks.functionaltest.APITestCase

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class StudiesResourceTests extends APITestCase {

    void testListAllStudies() {
        get("${baseURL}studies")
        assertStatus 200

        assertThat JSON, contains(
                allOf(
                        hasEntry('name', 'STUDY1'),
                        hasEntry(is('ontologyTerm'), allOf(
                                hasEntry('name', 'study1'),
                                hasEntry('fullName', '\\foo\\study1\\'),
                                hasEntry('key', '\\\\i2b2 main\\foo\\study1\\'),
                        ))
                ),
                allOf(
                        hasEntry('name', 'STUDY2'),
                        hasEntry(is('ontologyTerm'), allOf(
                                hasEntry('name', 'study2'),
                                hasEntry('fullName', '\\foo\\study2\\'),
                                hasEntry('key', '\\\\i2b2 main\\foo\\study2\\'),
                        ))
                )
        )
    }

    void testGetStudy() {
        def studyName = 'STUDY1'
        get("${baseURL}studies/${studyName}")
        assertStatus 200

        assertThat JSON, allOf(
                hasEntry('name', 'STUDY1'),
                hasEntry(is('ontologyTerm'), allOf(
                        hasEntry('name', 'study1'),
                        hasEntry('fullName', '\\foo\\study1\\'),
                        hasEntry('key', '\\\\i2b2 main\\foo\\study1\\'),
                ))
        )
    }

    /*FIXME Response contains null as content
    void testGetNonExistentStudy() {
        def studyName = 'STUDY_NOT_EXIST'
        get("${baseURL}studies/${studyName}")
        assertStatus 404

        assertThat JSON, allOf(
                hasEntry('httpStatus', 404),
                hasEntry('type', 'NoSuchResourceException'),
                hasEntry('message', "No study with name '${studyName}' was found"),
        )
    }*/

}
