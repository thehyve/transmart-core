package org.transmartproject.core.users

import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.greaterThan
import static org.transmartproject.core.users.PatientDataAccessLevel.*

class AccessLevelTests {

    @Test
    void testViewPermissionIsHigherThenAggregateWithThreshold() {
        assertThat SUMMARY, greaterThan(COUNTS_WITH_THRESHOLD)
    }

    @Test
    void testExportPermissionIsHigherThenView() {
        assertThat MEASUREMENTS, greaterThan(SUMMARY)
    }

}
