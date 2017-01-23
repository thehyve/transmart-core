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

package org.transmartproject.db.dataquery.highdim

import grails.test.mixin.TestMixin
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.querytool.Item
import org.transmartproject.core.querytool.Panel
import org.transmartproject.core.querytool.QueriesResource
import org.transmartproject.core.querytool.QueryDefinition
import org.transmartproject.db.dataquery.highdim.mrna.MrnaTestData
import org.transmartproject.db.ontology.I2b2
import org.transmartproject.db.ontology.StudyTestData
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import javax.annotation.Resource

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@TestMixin(RuleBasedIntegrationTestMixin)
class HighDimensionDataTypeResourceTests {

    @Resource
    HighDimensionDataTypeModule mrnaModule

    HighDimensionDataTypeResource resource
    QueriesResource queriesResourceService


    I2b2 i2b2Node

    @Before
    void setUp() {
        StudyTestData studyTestData = new StudyTestData()
        studyTestData.saveAll()
        i2b2Node = studyTestData.i2b2List[0]

        MrnaTestData testData = new MrnaTestData(conceptCode: i2b2Node.code, patients: studyTestData.i2b2Data.patients)
        testData.saveAll()

        assertThat mrnaModule, is(notNullValue())

        resource = new HighDimensionDataTypeResourceImpl(mrnaModule)
    }

    @Test
    void basicTest() {
        def definition = new QueryDefinition([
                new Panel(
                        items: [
                                new Item(
                                        conceptKey: i2b2Node.key.toString()
                                )
                        ]
                )
        ])

        def result = queriesResourceService.runQuery(definition)
        def ontologyTerms = resource.getAllOntologyTermsForDataTypeBy(result)

        assertThat ontologyTerms, allOf(
                hasSize(1),
                contains(
                        hasProperty('key', equalTo(i2b2Node.key.toString()))
                )
        )
    }
}
