package org.transmartproject.core.users

import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.greaterThan
import static org.transmartproject.core.users.AccessLevel.*

class AccessLevelTests {

    @Test
    void testViewPermissionIsHigherThenAggregateWithThreshold() {
        assertThat VIEW, greaterThan(AGGREGATE_WITH_THRESHOLD)
    }

    @Test
    void testExportPermissionIsHigherThenView() {
        assertThat EXPORT, greaterThan(VIEW)
    }

    @Test
    void testOwnPermissionIsHigherThenExport() {
        assertThat OWN, greaterThan(EXPORT)
    }

}
