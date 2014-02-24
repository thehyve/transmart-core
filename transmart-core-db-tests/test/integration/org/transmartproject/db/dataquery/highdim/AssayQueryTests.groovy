package org.transmartproject.db.dataquery.highdim

import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.db.dataquery.highdim.assayconstraints.DefaultTrialNameConstraint

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.db.test.Matchers.hasSameInterfaceProperties

/**
 * Created by glopes on 11/23/13.
 */
class AssayQueryTests {

    private AssayQuery testee

    AssayTestData testData = new AssayTestData()

    @Before
    void setUp() {
        testData.saveAll()
    }

    @Test
    void testPrepareCriteriaWithConstraints() {
        testee = new AssayQuery([
                new DefaultTrialNameConstraint(trialName: 'SAMPLE_TRIAL_2')
        ])
        def criteriaBuilder = testee.prepareCriteriaWithConstraints()
        List results = criteriaBuilder.instance.list()

        assertThat results, containsInAnyOrder(
                testData.assays[6],
                testData.assays[7],
                testData.assays[8])
    }

    @Test
    void testRetrieveAssays() {
        testee = new AssayQuery([
                new DefaultTrialNameConstraint(trialName: 'SAMPLE_TRIAL_2')
        ])

        List results = testee.retrieveAssays()

        assertThat results, allOf(
                everyItem(isA(Assay)),
                contains( /* order is asc */
                        hasSameInterfaceProperties(Assay, testData.assays[8]),
                        hasSameInterfaceProperties(Assay, testData.assays[7]),
                        hasSameInterfaceProperties(Assay, testData.assays[6]),
                )
        )
    }
}
