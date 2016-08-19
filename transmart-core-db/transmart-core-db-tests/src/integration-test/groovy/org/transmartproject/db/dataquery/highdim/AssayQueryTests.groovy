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

package org.transmartproject.db.dataquery.highdim

import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.db.dataquery.highdim.assayconstraints.DefaultTrialNameCriteriaConstraint

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.db.test.Matchers.hasSameInterfaceProperties

/**
 * Created by glopes on 11/23/13.
 */
class AssayQueryTests {

    AssayTestData testData = new AssayTestData()

    @Before
    void setUp() {
        testData.saveAll()
    }

    @Test
    void testPrepareCriteriaWithConstraints() {
        List results = new AssayQuery([new DefaultTrialNameCriteriaConstraint(trialName: 'SAMPLE_TRIAL_2')]).list()

        assertThat results, containsInAnyOrder(
                testData.assays[6],
                testData.assays[7],
                testData.assays[8])
    }

    @Test
    void testRetrieveAssays() {
        List results = new AssayQuery([new DefaultTrialNameCriteriaConstraint(trialName: 'SAMPLE_TRIAL_2')]).list()

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
