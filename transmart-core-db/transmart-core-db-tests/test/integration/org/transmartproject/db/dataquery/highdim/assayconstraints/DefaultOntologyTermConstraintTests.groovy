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

package org.transmartproject.db.dataquery.highdim.assayconstraints

import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.db.dataquery.highdim.AssayQuery
import org.transmartproject.db.dataquery.highdim.AssayTestData

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class DefaultOntologyTermConstraintTests {

    ConceptsResource conceptsResourceService

    AssayTestData testData = new AssayTestData()

    @Before
    void setUp() {
        testData.saveAll()
    }

    @Test
    void basicTest() {
        List<Assay> assays = new AssayQuery([
                new DefaultOntologyTermCriteriaConstraint(
                        term: conceptsResourceService.getByKey('\\\\i2b2 main\\foo\\bar')
                )
        ]).list()

        /* We should have gotten the assays in the -200 range.
         * Those in the other ranges are assigned to another concept
         */
        assertThat assays, containsInAnyOrder(
                hasProperty('id', equalTo(-201L)),
                hasProperty('id', equalTo(-202L)),
                hasProperty('id', equalTo(-203L)),
        )
    }

    @Test
    void testOntologyTermConstraintSupportsDisjunctions() {
        List<Assay> assays = new AssayQuery([
                new DisjunctionAssayCriteriaConstraint(constraints: [
                        new DefaultTrialNameCriteriaConstraint(trialName: 'bad name'),
                        new DefaultOntologyTermCriteriaConstraint(
                                term: conceptsResourceService.getByKey('\\\\i2b2 main\\foo\\bar')
                        )])]).list()

        assertThat assays, hasSize(3) /* see basic test */
    }

}
