package org.transmartproject.db.ontology

import grails.test.mixin.TestMixin
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.ontology.Study
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.db.ontology.ConceptTestData.createI2b2Concept

@TestMixin(RuleBasedIntegrationTestMixin)
class StudiesResourceServiceTests {

    StudyTestData studyTestData = new StudyTestData()

    StudiesResource studiesResourceService

    ConceptsResourceService conceptsResourceService

    @Before
    void setUp() {
        studyTestData.saveAll()
    }

    @Test
    void testGetStudySet() {
        def result = studiesResourceService.studySet

        assertThat result, allOf(
                everyItem(isA(Study)),
                containsInAnyOrder(
                        allOf(
                                hasProperty('id', is('STUDY_ID_1')),
                                hasProperty('ontologyTerm',
                                    hasProperty('fullName', is('\\foo\\study1\\')))),
                        allOf(
                                hasProperty('id', is('STUDY_ID_2')),
                                hasProperty('ontologyTerm',
                                    hasProperty('fullName', is('\\foo\\study2\\')))),
                        allOf(
                                hasProperty('id', is('STUDY_ID_3')),
                                hasProperty('ontologyTerm',
                                        hasProperty('fullName', is('\\foo\\study3\\'))))))
    }

    @Test
    void testGetStudyById() {
        // shouldn't get confused with \foo\study2\study1
        def result = studiesResourceService.getStudyById('study_id_1')

        assertThat result, allOf(
                hasProperty('id', is('STUDY_ID_1')),
                hasProperty('ontologyTerm',
                    allOf(
                        hasProperty('name', is('study1')),
                        hasProperty('fullName', is('\\foo\\study1\\'))
                    )
                )
        )
    }

    @Test
    void testGetStudyByIdDifferentCase() {
        def result = studiesResourceService.getStudyById('stuDY_Id_1')

        assertThat result, allOf(
                hasProperty('id', is('STUDY_ID_1')),
                hasProperty('ontologyTerm',
                        allOf(
                                hasProperty('name', is('study1')),
                                hasProperty('fullName', is('\\foo\\study1\\'))
                        )
                )
        )
    }

    @Test
    void testGetStudyByNameNonExistent() {
        shouldFail NoSuchResourceException, {
            studiesResourceService.getStudyById('bad study id')
        }
    }

    @Test
    void testGetStudyByOntologyTerm() {
        def concept = conceptsResourceService.getByKey('\\\\i2b2 main\\foo\\study1\\')

        def result = studiesResourceService.getStudyByOntologyTerm(concept)

        assertThat result, allOf(
                hasProperty('id', is('STUDY_ID_1')),
                hasProperty('ontologyTerm',
                        allOf(
                                hasProperty('name', is('study1')),
                                hasProperty('fullName', is('\\foo\\study1\\'))
                        )
                )
        )
    }

    @Test
    void testGetStudyByOntologyTermOptimization() {
        /* Terms marked with the"Study" visual attribute can be assumed to
         * refer to studies, a fact for which we optimize */
        I2b2 concept = createI2b2Concept(code: -9999, level: 1,
                fullName: '\\foo\\Study Visual Attribute\\',
                name: 'Study Visual Attribute',
                cComment: 'trial:ST_VIS_ATTR',
                cVisualattributes: 'FAS')

        assertThat concept.save(), is(notNullValue())

        def result = studiesResourceService.getStudyByOntologyTerm(concept)

        assertThat result, allOf(
                hasProperty('id', is('ST_VIS_ATTR')),
                hasProperty('ontologyTerm',
                        allOf(
                                hasProperty('name', is('Study Visual Attribute')),
                                hasProperty('fullName', is('\\foo\\Study Visual Attribute\\'))
                        )
                )
        )
    }

    @Test
    void testGetStudyByOntologyTermBadTerm() {
        def concept = conceptsResourceService.getByKey('\\\\i2b2 main\\foo\\study1\\bar\\')

        shouldFail NoSuchResourceException, {
            studiesResourceService.getStudyByOntologyTerm(concept)
        }
    }

}
