package org.transmartproject.db.ontology

import grails.test.mixin.TestMixin
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.ontology.Study
import org.transmartproject.db.i2b2data.I2b2Data
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.containsInAnyOrder
import static org.hamcrest.Matchers.everyItem
import static org.hamcrest.Matchers.hasProperty
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.isA

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
                                hasProperty('name', is('STUDY1')),
                                hasProperty('ontologyTerm',
                                    hasProperty('fullName', is('\\foo\\study1\\')))),
                        allOf(
                                hasProperty('name', is('STUDY2')),
                                hasProperty('ontologyTerm',
                                        hasProperty('fullName', is('\\foo\\study2\\'))))))
    }

    @Test
    void testGetStudyByName() {
        // shouldn't get confused with \foo\study2\study1
        def result = studiesResourceService.getStudyByName('study1')

        assertThat result, hasProperty('ontologyTerm',
                hasProperty('fullName', is('\\foo\\study1\\')))
    }

    @Test
    void testGetStudyByNameDifferentCase() {
        def result = studiesResourceService.getStudyByName('stuDY1')

        assertThat result, hasProperty('ontologyTerm',
                hasProperty('fullName', is('\\foo\\study1\\')))
    }

    @Test
    void testGetStudyByNameNonExistent() {
        shouldFail NoSuchResourceException, {
            studiesResourceService.getStudyByName('bad study name')
        }
    }

    @Test
    void testGetStudyByOntologyTerm() {
        def concept = conceptsResourceService.getByKey('\\\\i2b2 main\\foo\\study1\\')

        def result = studiesResourceService.getStudyByOntologyTerm(concept)

        assertThat result, hasProperty('ontologyTerm', is(concept))
    }

    @Test
    void testGetStudyByOntologyTermBadTerm() {
        def concept = conceptsResourceService.getByKey('\\\\i2b2 main\\foo\\study1\\bar\\')

        shouldFail NoSuchResourceException, {
            studiesResourceService.getStudyByOntologyTerm(concept)
        }
    }

}
