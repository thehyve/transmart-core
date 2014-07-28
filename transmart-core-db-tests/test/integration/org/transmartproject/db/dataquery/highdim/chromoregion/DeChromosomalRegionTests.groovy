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

package org.transmartproject.db.dataquery.highdim.chromoregion

import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.Platform
import org.transmartproject.core.dataquery.highdim.chromoregion.Region
import org.transmartproject.db.dataquery.highdim.acgh.AcghTestData

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class DeChromosomalRegionTests {

    AcghTestData testData = new AcghTestData()

    @Before
    void setUp() {
        assertThat testData.regionPlatform.save(), isA(Platform)
        assertThat testData.regions*.save(), contains(
                isA(Region), isA(Region)
        )
    }


    @Test
    void testBasicDataFetch() {
        Region r = DeChromosomalRegion.get(testData.regions[0].id)

        assertThat r, allOf(
                is(notNullValue()),
                hasProperty('chromosome', equalTo('1')),
                hasProperty('start', equalTo(33L)),
                hasProperty('end', equalTo(9999L)),
                hasProperty('numberOfProbes', equalTo(42)),
                hasProperty('name', equalTo('region 1:33-9999'))
        )
    }

    @Test
    void testGetPlatform() {
        Region r = DeChromosomalRegion.get(testData.regions[0].id)

        assertThat r, is(notNullValue())

        assertThat r.platform, allOf(
                is(notNullValue()),
                hasProperty('id', equalTo(testData.regionPlatform.id))
        )
    }

}
