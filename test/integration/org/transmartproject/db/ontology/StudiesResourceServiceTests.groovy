package org.transmartproject.db.ontology

import org.junit.Before
import org.junit.Test
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.ontology.Study

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.containsInAnyOrder
import static org.hamcrest.Matchers.everyItem
import static org.hamcrest.Matchers.hasProperty
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.isA

@Mixin(ConceptTestData)
class StudiesResourceServiceTests {

    StudiesResource studiesResourceService

    ConceptsResourceService conceptsResourceService

    @Before
    void setUp() {
        addTableAccess(level: 0, fullName: '\\foo\\', name: 'foo',
                tableCode: 'i2b2 main', tableName: 'i2b2')

        addI2b2(level: 1, fullName: '\\foo\\study1\\',         name: 'study1', cVisualattributes: 'FAS')
        addI2b2(level: 2, fullName: '\\foo\\study1\\bar\\',    name: 'bar',    cVisualattributes: 'LA')

        addI2b2(level: 1, fullName: '\\foo\\study2\\',         name: 'study2', cVisualattributes: 'FAS')
        addI2b2(level: 2, fullName: '\\foo\\study2\\study1\\', name: 'study1', cVisualattributes: 'LA')
    }

    @Test
    void testGetStudySet() {
        def result = studiesResourceService.studySet

        assertThat result, allOf(
                everyItem(isA(Study)),
                containsInAnyOrder(
                        hasProperty('ontologyTerm',
                                hasProperty('fullName', is('\\foo\\study1\\'))),
                        hasProperty('ontologyTerm',
                                hasProperty('fullName', is('\\foo\\study2\\')))))
    }

    @Test
    void testGetStudyByName() {
        // shouldn't get confused with \foo\study2\study1
        def result = studiesResourceService.getStudyByName('study1')

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
