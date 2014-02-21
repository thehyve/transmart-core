package org.transmartproject.db.dataquery.highdim.assayconstraints

import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.db.dataquery.highdim.AssayQuery
import org.transmartproject.db.dataquery.highdim.AssayTestData

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsInAnyOrder
import static org.hamcrest.Matchers.is
import static org.transmartproject.test.Matchers.hasSameInterfaceProperties

class AssayIdListConstraintTests {

    AssayTestData testData = new AssayTestData()

    @Before
    void setUp() {
        testData.saveAll()
    }

    @Test
    void basicTest() {
        def wantedAssays = testData.assays.findAll {
            it.id == -201 || it.id == -301
        }

        AssayQuery assayQuery = new AssayQuery([
                new AssayIdListConstraint(
                        ids: wantedAssays*.id
                )
        ])

        assertThat assayQuery.retrieveAssays(), containsInAnyOrder(
                wantedAssays.collect {
                    hasSameInterfaceProperties(Assay, it)
                })
    }

}
