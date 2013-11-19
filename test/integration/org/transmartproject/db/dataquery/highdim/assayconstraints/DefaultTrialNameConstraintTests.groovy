package org.transmartproject.db.dataquery.highdim.assayconstraints

import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.db.dataquery.highdim.AssayQuery
import org.transmartproject.db.dataquery.highdim.AssayTestData

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class DefaultTrialNameConstraintTests {
    @Before
    void setup() {
        AssayTestData.saveAll()
    }

    @Test
    void basicTest() {
        AssayQuery assayQuery = new AssayQuery([
                new DefaultTrialNameConstraint(trialName: 'SAMPLE_TRIAL_2')
        ])

        List<AssayColumn> assays = assayQuery.retrieveAssays()

        assertThat assays, allOf(
                everyItem(
                        hasProperty('trialName', equalTo('SAMPLE_TRIAL_2'))
                ),
                containsInAnyOrder(
                        /* see test data */
                        hasProperty('id', equalTo(-401L)),
                        hasProperty('id', equalTo(-402L)),
                        hasProperty('id', equalTo(-403L)),
                )
        )
    }
}
