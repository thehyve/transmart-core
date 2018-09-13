package org.transmartproject.core.users

import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.greaterThan
import static org.transmartproject.core.users.PatientDataAccessLevel.*

class AccessLevelTests {

    @Test
    void testCountsPermissionIsHigherThenCountsWithThreshold() {
        assertThat COUNTS, greaterThan(COUNTS_WITH_THRESHOLD)
    }

    @Test
    void testSummaryPermissionIsHigherThenCountsWithThreshold() {
        assertThat SUMMARY, greaterThan(COUNTS)
    }

    @Test
    void testMeasurementsPermissionIsHigherThenSummary() {
        assertThat MEASUREMENTS, greaterThan(SUMMARY)
    }

}
