package org.transmartproject.rest

import com.grailsrocks.functionaltest.APITestCase
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.transmartproject.db.ontology.ConceptTestData
import org.transmartproject.db.ontology.I2b2
import org.transmartproject.db.ontology.StudyTestData
import org.transmartproject.rest.marshallers.OntologyTermSerializationHelper

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class SubjectsResourceTests extends ResourceTestCase {

    def study = 'study1'
    def defaultTrial = study.toUpperCase()
    def subjectId = -101
    def UNKNOWN = 'UNKOWN' // funny typo here
    def concept = OntologyTermSerializationHelper.pathToId('bar')

    def subjectsPerConceptUrl = "/studies/${study}/concepts/${concept}/subjects"
    def subjectsPerStudyUrl = "/studies/${study}/subjects"

    def subjectUrl = "/studies/${study}/subjects/${subjectId}"
    def subjectUrl2 = "/studies/${study}/subjects/-102"
    def subjectUrl3 = "/studies/${study}/subjects/-103"

    void testShowAsJson() {
        def result = getAsJson subjectUrl
        assertStatus 200
        assertThat result, hasJsonSubject()
    }

    void testShowAsHal() {
        def result = getAsHal subjectUrl
        assertStatus 200
        assertThat result, hasHalSubject()
    }

    void testIndexPerStudyAsJson() {

        def result = getAsJson subjectsPerStudyUrl
        assertStatus 200
        assertThat result,
                hasEntry(is('subjects'),
                        containsInAnyOrder(
                                hasJsonSubject(),
                                hasJsonSubject2(),
                                hasJsonSubject3(),
                        )
                )
    }

    void testIndexPerStudyAsHal() {

        def result = getAsHal subjectsPerStudyUrl
        assertStatus 200
        assertThat result,
                hasHalIndex(
                        subjectsPerStudyUrl,
                        ['subjects': containsInAnyOrder(
                                hasHalSubject(),
                                hasHalSubject2(),
                                hasHalSubject3(),
                        )]
                )

    }

    void testIndexPerConceptAsJson() {
        def result = getAsJson subjectsPerConceptUrl
        assertStatus 200

        assertThat result, hasEntry(is('subjects'),
                containsInAnyOrder(
                        hasJsonSubject(),
                )
        )
    }

    void testIndexPerConceptAsHal() {
        def result = getAsHal subjectsPerConceptUrl
        assertStatus 200

        assertThat result,
                hasHalIndex(
                        subjectsPerConceptUrl,
                        ['subjects': containsInAnyOrder(
                            hasHalSubject()
                        )]
                )
    }

    def hasJsonSubject2() {
        hasJsonSubject(-102, defaultTrial, 'SUBJ_ID_2')
    }

    def hasJsonSubject3() {
        hasJsonSubject(-103, defaultTrial, 'SUBJ_ID_3')
    }

    def hasHalSubject2() {
        hasHalSubject(-102, defaultTrial, 'SUBJ_ID_2', subjectUrl2)
    }

    def hasHalSubject3() {
        hasHalSubject(-103, defaultTrial, 'SUBJ_ID_3', subjectUrl3)
    }

    def hasJsonSubject(int id = subjectId,
                       String trial = defaultTrial,
                       String inTrialId = 'SUBJ_ID_1') {
        allOf(
                hasEntry('id', id),
                hasEntry('sex', UNKNOWN),
                hasEntry('trial', trial),
                hasEntry('inTrialId', inTrialId),
                hasEntry('religion', null),
                hasEntry('age', null),
                hasEntry('birthDate', null),
                hasEntry('maritalStatus', null),
                hasEntry('deathDate', null),
                hasEntry('race', null),
        )
    }

    def hasHalSubject(int id = subjectId,
                      String trial = defaultTrial,
                      String inTrialId = 'SUBJ_ID_1',
                      String selfLink = subjectUrl) {
        allOf(
                hasJsonSubject(id, trial, inTrialId),
                hasSelfLink(selfLink)
        )
    }

    /*//FIXME response contains null
    void testGetNonExistentStudy() {
        def studyName = 'STUDY_NOT_EXIST'
        get("${baseURL}studies/${studyName}/subjects")
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
        get("${baseURL}studies/${study}/subjects/${patientNum}")
        assertStatus 404

        assertThat JSON, allOf(
                hasEntry('httpStatus', 404),
                hasEntry('type', 'NoSuchResourceException'),
                hasEntry('message', "No subject with id '${studyName}' was found"),
        )
    }*/
}

