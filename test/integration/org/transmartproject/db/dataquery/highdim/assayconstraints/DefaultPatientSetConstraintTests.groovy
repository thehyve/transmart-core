package org.transmartproject.db.dataquery.highdim.assayconstraints

import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.AssayColumn
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
        AssayQuery assayQuery = new AssayQuery([
                new DefaultPatientSetConstraint(
                        queryResult: firstPatientResult
                )
        ])

        List<AssayColumn> assays = assayQuery.retrieveAssays()

        assertThat assays, allOf(
                everyItem(
                        hasProperty('patient', equalTo(testData.patients[0]))
                ),
                containsInAnyOrder(
                        /* see test data, -X01 ids are assays for the 1st patient */
                        hasProperty('id', equalTo(-201L)),
                        hasProperty('id', equalTo(-301L)),
                        hasProperty('id', equalTo(-401L)),
                )
        )
    }

    @Test
    void testPatientSetConstraintSupportsDisjunctions() {
        AssayQuery assayQuery = new AssayQuery([
                new DisjunctionAssayConstraint(constraints: [
                        new DefaultTrialNameConstraint(trialName: 'bad name'),
                        new DefaultPatientSetConstraint(
                                queryResult: firstPatientResult
                        )])])

        List<AssayColumn> assays = assayQuery.retrieveAssays()

        assertThat assays, hasSize(3) /* see basic test */
    }
}
