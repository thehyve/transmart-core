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
import grails.transaction.Rollback
import org.transmartproject.core.dataquery.highdim.GenomeBuildNumber
import spock.lang.Specification

import static org.hamcrest.Matchers.*

@Integration
@Rollback
class DeGplInfoSpec extends Specification {

    SampleHighDimTestData testData = new SampleHighDimTestData()

    void testScalarProperties() {
        assert testData.platform.save() != null

        when:
        def platform = DeGplInfo.get(testData.platform.id)

        then:
        platform allOf(
                is(notNullValue()),
                hasProperty('markerType', equalTo('generic')),
                hasProperty('title', equalTo('Test Generic Platform')),
                hasProperty('organism', equalTo('Homo Sapiens')),
                hasProperty('genomeReleaseId', equalTo('hg18')),
                hasProperty('annotationDate', equalTo(Date.parse('yyyy-MM-dd', '2013-05-03'))),
        )

        GenomeBuildNumber.forId(platform.genomeReleaseId) is(GenomeBuildNumber.GRCh36)
    }
}
