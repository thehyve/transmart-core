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

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.transmartproject.core.dataquery.highdim.Platform
import org.transmartproject.core.dataquery.highdim.chromoregion.Region
import org.transmartproject.db.dataquery.highdim.acgh.AcghTestData
import spock.lang.Specification

import static org.hamcrest.Matchers.*

@Integration
@Rollback

class DeChromosomalRegionSpec extends Specification {

    AcghTestData testData = new AcghTestData()

    void setupData() {
        assert testData.regionPlatform.save() instanceof Platform
        testData.regions*.save()
        //testData.regions.each { assert it instanceof Region}
    }


    void testBasicDataFetch() {
        setupData()
        Region r = DeChromosomalRegion.get(testData.regions[0].id)

        expect:
        r allOf(
                is(notNullValue()),
                hasProperty('chromosome', equalTo('1')),
                hasProperty('start', equalTo(33L)),
                hasProperty('end', equalTo(9999L)),
                hasProperty('numberOfProbes', equalTo(42)),
                hasProperty('name', equalTo('region 1:33-9999'))
        )
    }

    void testGetPlatform() {
        setupData()
        Region r = DeChromosomalRegion.get(testData.regions[0].id)

        expect:
        r is(notNullValue())

        r.platform allOf(
                is(notNullValue()),
                hasProperty('id', equalTo(testData.regionPlatform.id))
        )
    }

}
