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

package org.transmartproject.db.dataquery.highdim.acgh

import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.acgh.ChromosomalSegment
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.exceptions.EmptySetException
import org.transmartproject.db.dataquery.highdim.HighDimTestData

import static groovy.util.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save
/**
 * Created by glopes on 11/23/13.
 */
class AcghDataTypeResourceTests {

    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource acghResource

    AcghTestData testData = new AcghTestData()

    @Before
    void setUp() {
        acghResource = highDimensionResourceService.getSubResourceForType 'acgh'
    }

    @Test
    void testAcghModuleGivesBackResourceSubtype() {
        assertThat acghResource, isA(AcghDataTypeResource)
    }

    @Test
    void testChromosomalSegmentsBasic() {
        AcghDataTypeResource resource = acghResource

        testData.saveAll()

        def assayConstraints = [
                resource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: testData.allPatientsQueryResult.id),
        ]

        List<ChromosomalSegment> result =
                resource.retrieveChromosomalSegments assayConstraints

        assertThat result, containsInAnyOrder(
                new ChromosomalSegment(chromosome: '2', start: 66L, end: 99L),
                new ChromosomalSegment(chromosome: '1', start: 33L, end: 9999L))
    }

    @Test
    void testChromosomalSegmentsNoAssays() {
        testData.saveAll()

        def assayConstraints = [
                acghResource.createAssayConstraint(
                        AssayConstraint.TRIAL_NAME_CONSTRAINT,
                        name: 'trial name that does not exist'),
        ]

        def exception = shouldFail EmptySetException, {
            acghResource.retrieveChromosomalSegments assayConstraints
        }
        assertThat exception, hasProperty('message',
                containsString('No assays satisfy'))
    }

    @Test
    void testChromosomalSegmentsEmptyPlatform() {
        def trialName = 'bogus trial'
        testData.saveAll()
        def testAssays =  HighDimTestData.createTestAssays(
                testData.patients,
                -60000L,
                testData.bogusTypePlatform,
                trialName)
        save testAssays

        def assayConstraints = [
                acghResource.createAssayConstraint(
                        AssayConstraint.TRIAL_NAME_CONSTRAINT,
                        name: trialName),
        ]

        def exception = shouldFail EmptySetException, {
            acghResource.retrieveChromosomalSegments assayConstraints
        }
        assertThat exception, hasProperty('message',
                containsString('No regions found for'))
    }

    @Test
    void testAcghPlatformIsRecognized() {
        def constraint = highDimensionResourceService.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: AcghTestData.TRIAL_NAME)

        testData.saveAll()

        def map = highDimensionResourceService.
                getSubResourcesAssayMultiMap([constraint])

        assertThat map, hasKey(
                hasProperty('dataTypeName', equalTo('acgh')))

        def entry = map.entrySet().find { it.key.dataTypeName == 'acgh' }

        assertThat entry.value, allOf(
                hasSize(greaterThan(0)),
                everyItem(
                        hasProperty('platform',
                                hasProperty('markerType',
                                        equalTo(AcghTestData.ACGH_PLATFORM_MARKER_TYPE)))))
    }

}
