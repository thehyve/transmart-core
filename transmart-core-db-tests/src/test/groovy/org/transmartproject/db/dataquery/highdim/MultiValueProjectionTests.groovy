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

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.acgh.CopyNumberState
import org.transmartproject.core.dataquery.highdim.projections.AllDataProjection
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.dataquery.highdim.acgh.AcghValuesProjection
import org.transmartproject.db.dataquery.highdim.parameterproducers.AllDataProjectionFactory
import org.transmartproject.db.dataquery.highdim.rnaseq.RnaSeqValuesProjection

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo


@TestMixin(GrailsUnitTestMixin)
class MultiValueProjectionTests {

    @Test
    void testAllDataProjectionProperties() {
        Map<String, Class> dataProps = [foo:String, bar:Double]
        Map<String, Class> rowProps = [rowA:Double, rowB:String]

        AllDataProjectionFactory factory = new AllDataProjectionFactory(dataProps, rowProps)
        AllDataProjection projection = factory.createFromParameters(Projection.ALL_DATA_PROJECTION, [:], null)

        assertThat projection.dataProperties.entrySet(), equalTo(dataProps.entrySet())
    }

    @Test
    void testAcghProjectionProperties() {
        //the actual code is smarter than this, so any new property will requite test to be adjusted
        Map<String, Class> dataProps = [
                probabilityOfNormal: Double,
                probabilityOfAmplification: Double,
                copyNumberState: CopyNumberState,
                segmentCopyNumberValue: Double,
                probabilityOfGain: Double,
                chipCopyNumberValue: Double,
                probabilityOfLoss: Double]

        AcghValuesProjection projection = new AcghValuesProjection()
        assertThat projection.dataProperties.entrySet(), equalTo(dataProps.entrySet())
    }

    @Test
    void testRnaSeqProjectionProperties() {
        //the actual code is smarter than this, so any new property will requite test to be adjusted
        Map<String, Class> dataProps = [
                readcount:Integer,
                normalizedReadcount:Double,
                logNormalizedReadcount:Double,
                zscore:Double]

        RnaSeqValuesProjection projection = new RnaSeqValuesProjection()
        assertThat projection.dataProperties.entrySet(), equalTo(dataProps.entrySet())
    }

}
