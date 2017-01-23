package org.transmartproject.db.ontology

import grails.test.mixin.TestMixin
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTermTag
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@TestMixin(RuleBasedIntegrationTestMixin)
class OntologyTermTagsResourceServiceTests {

    OntologyTermTagsResourceService ontologyTermTagsResourceService
    ConceptsResource conceptsResourceService

    StudyTestData studyTestData = new StudyTestData()

    @Before
    void setUp() {
        studyTestData.saveAll()
    }

    @Test
    void testGetTagsShallow() {
        String key = '\\\\i2b2 main\\foo\\study1\\'
        def studyConcept = conceptsResourceService.getByKey(key)
        Map<OntologyTerm, List<OntologyTermTag>> tags = ontologyTermTagsResourceService
                .getTags([studyConcept] as Set, false)
        //note that reverse order here tests sorting by tag.position
        assertThat tags, hasEntry(
                hasProperty('key', equalTo(key)),
                contains(
                    allOf(
                        hasProperty('name', equalTo('1 name 2')),
                        hasProperty('description', equalTo('1 description 2')),
                    ),
                    allOf(
                        hasProperty('name', equalTo('1 name 1')),
                        hasProperty('description', equalTo('1 description 1')),
                    ),
                ))
    }

    @Test
    void testGetDiffTermsTagsShallow() {
        String key1 = '\\\\i2b2 main\\foo\\study1\\'
        String key2 = '\\\\i2b2 main\\foo\\study2\\'
        def study1Concept = conceptsResourceService.getByKey(key1)
        def study2Concept = conceptsResourceService.getByKey(key2)
        Map<OntologyTerm, List<OntologyTermTag>> tags = ontologyTermTagsResourceService
                .getTags([study1Concept, study2Concept] as Set, false)
        //note that reverse order here tests sorting by tag.position
        assertThat tags, allOf(
                hasEntry(
                        hasProperty('key', equalTo(key1)),
                        contains(
                                allOf(
                                        hasProperty('name', equalTo('1 name 2')),
                                        hasProperty('description', equalTo('1 description 2')),
                                ),
                                allOf(
                                        hasProperty('name', equalTo('1 name 1')),
                                        hasProperty('description', equalTo('1 description 1')),
                                ),
                        )),
                hasEntry(
                        hasProperty('key', equalTo(key2)),
                        contains(
                                allOf(
                                        hasProperty('name', equalTo('3 name 2')),
                                        hasProperty('description', equalTo('3 description 2')),
                                ),
                                allOf(
                                        hasProperty('name', equalTo('3 name 1')),
                                        hasProperty('description', equalTo('3 description 1')),
                                ),
                        )),
        )
    }

    @Test
    void testGetTagsDeep() {
        String key = '\\\\i2b2 main\\foo\\study1\\'
        def studyConcept = conceptsResourceService.getByKey(key)
        Map<OntologyTerm, List<OntologyTermTag>> tags = ontologyTermTagsResourceService
                .getTags([studyConcept] as Set, true)
        //note that reverse order here tests sorting by tag.position
        assertThat tags, allOf(
                hasEntry(
                    hasProperty('key', equalTo(key)),
                    contains(
                            allOf(
                                    hasProperty('name', equalTo('1 name 2')),
                                    hasProperty('description', equalTo('1 description 2')),
                            ),
                            allOf(
                                    hasProperty('name', equalTo('1 name 1')),
                                    hasProperty('description', equalTo('1 description 1')),
                            ),
                    )),
                hasEntry(
                    hasProperty('key', equalTo("${key}bar\\".toString())),
                    contains(
                            allOf(
                                    hasProperty('name', equalTo('2 name 2')),
                                    hasProperty('description', equalTo('2 description 2')),
                            ),
                            allOf(
                                    hasProperty('name', equalTo('2 name 1')),
                                    hasProperty('description', equalTo('2 description 1')),
                            ),
                    )),
        )
    }

}
