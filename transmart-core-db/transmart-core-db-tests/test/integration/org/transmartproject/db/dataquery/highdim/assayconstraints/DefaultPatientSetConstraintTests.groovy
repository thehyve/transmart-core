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
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.dataquery.highdim.AssayQuery
import org.transmartproject.db.dataquery.highdim.AssayTestData
import org.transmartproject.db.querytool.QtQueryMaster
import org.transmartproject.db.querytool.QueryResultData

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class DefaultPatientSetConstraintTests {

    /* patient set with only the first patient (AssayTestData.patients[0]) */
    QueryResult firstPatientResult

    AssayTestData testData = new AssayTestData()

    @Before
    void setup() {
        testData.saveAll()

        QtQueryMaster master = QueryResultData.createQueryResult([
                testData.patients[0]
        ])

        master.save()
        firstPatientResult = master.
                queryInstances.iterator().next(). // QtQueryInstance
                queryResults.iterator().next()
    }

    @Test
    void basicTest() {
        List<Assay> assays = new AssayQuery([
                new DefaultPatientSetCriteriaConstraint(
                        queryResult: firstPatientResult
                )
        ]).list()

        assertThat assays, allOf(
                everyItem(
                        hasProperty('patient', equalTo(testData.patients[0]))
                ),
                containsInAnyOrder(
                        /* see test data, -X01 ids are assays for the 1st patient */
                        hasProperty('id', equalTo(-201L)),
                        hasProperty('id', equalTo(-301L)),
                        hasProperty('id', equalTo(-401L)),
                        hasProperty('id', equalTo(-501L)),
                )
        )
    }

    @Test
    void testPatientSetConstraintSupportsDisjunctions() {
        List<Assay> assays = new AssayQuery([
                new DisjunctionAssayCriteriaConstraint(constraints: [
                        new DefaultTrialNameCriteriaConstraint(trialName: 'bad name'),
                        new DefaultPatientSetCriteriaConstraint(
                                queryResult: firstPatientResult
                        )])]).list()

        assertThat assays, hasSize(4) /* see basic test */
    }
}
