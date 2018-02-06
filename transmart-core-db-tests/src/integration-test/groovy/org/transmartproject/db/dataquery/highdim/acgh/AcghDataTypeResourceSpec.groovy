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

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.acgh.ChromosomalSegment
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.exceptions.EmptySetException
import org.transmartproject.db.TestData
import org.transmartproject.db.dataquery.highdim.HighDimTestData
import org.transmartproject.db.TransmartSpecification

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

/**
 * Created by glopes on 11/23/13.
 */

@Integration
@Rollback
class AcghDataTypeResourceSpec extends TransmartSpecification {

    @Autowired
    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource acghResource

    AcghTestData testData

    void setupData() {
        TestData.clearAllData()

        testData = new AcghTestData()
        acghResource = highDimensionResourceService.getSubResourceForType 'acgh'
    }

    void testAcghModuleGivesBackResourceSubtype() {
        setupData()

        expect:
        acghResource instanceof AcghDataTypeResource
    }

    void testChromosomalSegmentsBasic() {
        setupData()
        AcghDataTypeResource resource = acghResource

        testData.saveAll()

        def assayConstraints = [
                resource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: testData.allPatientsQueryResult.id),
        ]

        List<ChromosomalSegment> result =
                resource.retrieveChromosomalSegments assayConstraints

        expect:
        result.size() == 2
        new ChromosomalSegment(chromosome: '2', start: 66L, end: 99L) in result
        new ChromosomalSegment(chromosome: '1', start: 33L, end: 9999L) in result
    }

    void testChromosomalSegmentsNoAssays() {
        setupData()
        testData.saveAll()

        def assayConstraints = [
                acghResource.createAssayConstraint(
                        AssayConstraint.TRIAL_NAME_CONSTRAINT,
                        name: 'trial name that does not exist'),
        ]

        when:
        acghResource.retrieveChromosomalSegments assayConstraints
        then:
        def e = thrown(EmptySetException)
        e.message.contains('No assays satisfy')
    }

    void testChromosomalSegmentsEmptyPlatform() {
        setupData()
        def trialName = 'bogus trial'
        testData.saveAll()
        def testAssays = HighDimTestData.createTestAssays(
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

        when:
        acghResource.retrieveChromosomalSegments assayConstraints
        then:
        def e = thrown(EmptySetException)
        e.message.contains('No regions found for')
    }

    void testAcghPlatformIsRecognized() {
        setupData()
        def constraint = highDimensionResourceService.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: AcghTestData.TRIAL_NAME)

        testData.saveAll()

        when:
        def map = highDimensionResourceService.
                getSubResourcesAssayMultiMap([constraint])

        then:
        map.keySet().find { it.dataTypeName == 'acgh' }

        when:
        def entry = map.entrySet().find { it.key.dataTypeName == 'acgh' }

        then:
        !entry.value.empty
        entry.value.every { it.platform.markerType == AcghTestData.ACGH_PLATFORM_MARKER_TYPE }
    }

}
