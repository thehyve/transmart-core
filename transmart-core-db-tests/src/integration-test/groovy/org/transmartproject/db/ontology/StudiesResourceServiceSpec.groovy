/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.ontology

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.OntologyTermsResource
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.ontology.Study
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static org.transmartproject.db.ontology.ConceptTestData.createI2b2Concept

@Integration
@Rollback
class StudiesResourceServiceSpec extends Specification {

    TabularStudyTestData studyTestData

    StudiesResource studiesResourceService

    OntologyTermsResource ontologyTermsResourceService

    void setupData() {
        studyTestData = new TabularStudyTestData()
        studyTestData.saveAll()
    }

    void testGetStudySet() {
        setupData()
        def result = studiesResourceService.studySet

        expect:
        result allOf(
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

    void testGetStudyById() {
        setupData()
        // shouldn't get confused with \foo\study2\study1
        def result = studiesResourceService.getStudyById('study_id_1')

        expect:
        result allOf(
                hasProperty('id', is('STUDY_ID_1')),
                hasProperty('ontologyTerm',
                        allOf(
                                hasProperty('name', is('study1')),
                                hasProperty('fullName', is('\\foo\\study1\\'))
                        )
                )
        )
    }

    void testGetStudyByIdDifferentCase() {
        setupData()
        def result = studiesResourceService.getStudyById('stuDY_Id_1')

        expect:
        result allOf(
                hasProperty('id', is('STUDY_ID_1')),
                hasProperty('ontologyTerm',
                        allOf(
                                hasProperty('name', is('study1')),
                                hasProperty('fullName', is('\\foo\\study1\\'))
                        )
                )
        )
    }

    void testGetStudyByNameNonExistent() {
        setupData()

        when:
        studiesResourceService.getStudyById('bad study id')
        then:
        thrown(NoSuchResourceException)
    }

    void testGetStudyByOntologyTerm() {
        setupData()
        def concept = ontologyTermsResourceService.getByKey('\\\\i2b2 main\\foo\\study1\\')

        def result = studiesResourceService.getStudyByOntologyTerm(concept)

        expect:
        result allOf(
                hasProperty('id', is('STUDY_ID_1')),
                hasProperty('ontologyTerm',
                        allOf(
                                hasProperty('name', is('study1')),
                                hasProperty('fullName', is('\\foo\\study1\\'))
                        )
                )
        )
    }

    void testGetStudyByOntologyTermOptimization() {
        setupData()
        /* Terms marked with the"Study" visual attribute can be assumed to
         * refer to studies, a fact for which we optimize */
        I2b2 concept = createI2b2Concept(code: -9999, level: 1,
                fullName: '\\foo\\Study Visual Attribute\\',
                name: 'Study Visual Attribute',
                cComment: 'trial:ST_VIS_ATTR',
                cVisualattributes: 'FAS')

        concept.save()

        when:
        def result = studiesResourceService.getStudyByOntologyTerm(concept)

        then:
        result allOf(
                hasProperty('id', is('ST_VIS_ATTR')),
                hasProperty('ontologyTerm',
                        allOf(
                                hasProperty('name', is('Study Visual Attribute')),
                                hasProperty('fullName', is('\\foo\\Study Visual Attribute\\'))
                        )
                )
        )
    }

    void testGetStudyByOntologyTermBadTerm() {
        setupData()
        def concept = ontologyTermsResourceService.getByKey('\\\\i2b2 main\\foo\\study1\\bar\\')

        when:
        studiesResourceService.getStudyByOntologyTerm(concept)
        then:
        thrown(NoSuchResourceException)
    }

}
