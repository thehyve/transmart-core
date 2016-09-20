package org.transmartproject.db.ontology

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTermTag
import spock.lang.Specification

import static org.hamcrest.Matchers.*

@Integration
@Rollback
class OntologyTermTagsResourceServiceSpec extends Specification {

    OntologyTermTagsResourceService ontologyTermTagsResourceService
    ConceptsResource conceptsResourceService

    StudyTestData studyTestData = new StudyTestData()

    void setupData() {
        studyTestData.saveAll()
    }

    void testGetTagsShallow() {
        setupData()
        String key = '\\\\i2b2 main\\foo\\study1\\'
        def studyConcept = conceptsResourceService.getByKey(key)
        Map<OntologyTerm, List<OntologyTermTag>> tags = ontologyTermTagsResourceService
                .getTags([studyConcept] as Set, false)
        //note that reverse order here tests sorting by tag.position
        expect:
        tags hasEntry(
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

    void testGetDiffTermsTagsShallow() {
        setupData()
        String key1 = '\\\\i2b2 main\\foo\\study1\\'
        String key2 = '\\\\i2b2 main\\foo\\study2\\'
        def study1Concept = conceptsResourceService.getByKey(key1)
        def study2Concept = conceptsResourceService.getByKey(key2)
        Map<OntologyTerm, List<OntologyTermTag>> tags = ontologyTermTagsResourceService
                .getTags([study1Concept, study2Concept] as Set, false)
        //note that reverse order here tests sorting by tag.position
        expect:
        tags allOf(
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

    void testGetTagsDeep() {
        setupData()
        String key = '\\\\i2b2 main\\foo\\study1\\'
        def studyConcept = conceptsResourceService.getByKey(key)
        Map<OntologyTerm, List<OntologyTermTag>> tags = ontologyTermTagsResourceService
                .getTags([studyConcept] as Set, true)
        //note that reverse order here tests sorting by tag.position
        expect:
        tags allOf(
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
