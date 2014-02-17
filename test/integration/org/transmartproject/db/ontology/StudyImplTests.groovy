package org.transmartproject.db.ontology

import org.junit.Before
import org.junit.Test
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.ontology.Study

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsInAnyOrder
import static org.hamcrest.Matchers.is

class StudyImplTests {

    StudyTestData studyTestData = new StudyTestData()

    StudiesResource studiesResourceService

    @Before
    void before() {
        studyTestData.saveAll()
    }

    @Test
    void testStudyGetAllPatients() {
        Study study = studiesResourceService.getStudyByName('study1')

        assertThat study.patients, containsInAnyOrder(studyTestData.i2b2Data.patients.collect { is it })
    }

    @Test
    void testStudyGetName() {
        Study study = studiesResourceService.getStudyByName('study1')

        assertThat study.name, is('STUDY1' /* term name in uppercase */)
    }

}
