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

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.TestData
import org.transmartproject.db.dataquery.highdim.AssayQuery
import org.transmartproject.db.dataquery.highdim.AssayTestData
import org.transmartproject.db.querytool.QtQueryMaster
import org.transmartproject.db.querytool.QueryResultData
import spock.lang.Specification
import spock.lang.Specification

import static org.hamcrest.Matchers.*

@Integration
@Rollback
class DefaultPatientSetConstraintSpec extends Specification {

    /* patient set with only the first patient (AssayTestData.patients[0]) */
    QueryResult firstPatientResult

    AssayTestData testData = new AssayTestData()

    void setupData() {
        TestData.prepareCleanDatabase()

        testData.saveAll()

        QtQueryMaster master = QueryResultData.createQueryResult('default-patient-set', [
                testData.patients[0]
        ])

        master.save()
        firstPatientResult = master.
                queryInstances.iterator().next(). // QtQueryInstance
                queryResults.iterator().next()
    }

    void testBasic() {
        setupData()
        List<Assay> assays = new AssayQuery([
                new DefaultPatientSetCriteriaConstraint(
                        queryResult: firstPatientResult
                )
        ]).list()

        expect:
        assays allOf(
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

    void testPatientSetConstraintSupportsDisjunctions() {
        setupData()
        List<Assay> assays = new AssayQuery([
                new DisjunctionAssayCriteriaConstraint(constraints: [
                        new DefaultTrialNameCriteriaConstraint(trialName: 'bad name'),
                        new DefaultPatientSetCriteriaConstraint(
                                queryResult: firstPatientResult
                        )])]).list()

        expect:
        assays hasSize(4) /* see basic test */
    }
}
