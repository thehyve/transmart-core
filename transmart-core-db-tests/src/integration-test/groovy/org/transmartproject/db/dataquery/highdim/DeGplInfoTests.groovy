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

import grails.test.mixin.integration.Integration
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.GenomeBuildNumber

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@Integration
class DeGplInfoTests {

    SampleHighDimTestData testData = new SampleHighDimTestData()

    @Test
    void testScalarProperties() {
        assertThat testData.platform.save(), is(notNullValue())

        def platform = DeGplInfo.get(testData.platform.id)

        assertThat platform, allOf(
                is(notNullValue()),
                hasProperty('markerType', equalTo('generic')),
                hasProperty('title', equalTo('Test Generic Platform')),
                hasProperty('organism', equalTo('Homo Sapiens')),
                hasProperty('genomeReleaseId', equalTo('hg18')),
                hasProperty('annotationDate', equalTo(Date.parse('yyyy-MM-dd', '2013-05-03'))),
        )

        assertThat GenomeBuildNumber.forId(platform.genomeReleaseId), is(GenomeBuildNumber.GRCh36)
    }
}
