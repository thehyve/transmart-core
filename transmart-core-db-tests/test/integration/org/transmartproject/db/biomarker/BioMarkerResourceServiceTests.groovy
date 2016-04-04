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

package org.transmartproject.db.biomarker

import com.google.common.collect.Lists
import com.google.common.collect.Sets
import grails.test.mixin.TestMixin
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.IterableResult
import org.transmartproject.core.biomarker.BioMarker
import org.transmartproject.core.biomarker.BioMarkerConstraint
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.SampleBioMarkerTestData
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@TestMixin(RuleBasedIntegrationTestMixin)
class BioMarkerResourceServiceTests {

    SampleBioMarkerTestData bioMarkerTestData = new SampleBioMarkerTestData()

    BioMarkerResourceService bioMarkerResourceService

    @Before
    void setUp() {
        bioMarkerTestData.saveAll()
    }

    @Test(expected = InvalidArgumentsException)
    void testCreateNonExistingConstraint() {
        bioMarkerResourceService.createConstraint('test', foo: 'bar')
    }

    @Test
    void testFetchAll() {
        IterableResult<BioMarker> result = bioMarkerResourceService.retrieveBioMarkers([])

        def resultList = Lists.newArrayList result
        result.close()
        assertThat resultList, hasSize(19)
    }

    @Test(expected = InvalidArgumentsException)
    void testPropertiesConstraintThrowExcOnNoParams() {
        IterableResult<BioMarker> result = bioMarkerResourceService.retrieveBioMarkers([
                bioMarkerResourceService.createConstraint([:], BioMarkerConstraint.PROPERTIES_CONSTRAINT)
        ])

        def resultList = Lists.newArrayList result
        result.close()
        assertThat resultList, hasSize(19)
    }

    @Test
    void testPropertiesConstraintWithId() {
        IterableResult<BioMarker> result = bioMarkerResourceService.retrieveBioMarkers([
                bioMarkerResourceService.createConstraint(BioMarkerConstraint.PROPERTIES_CONSTRAINT, id: -1101L)
        ])

        def resultList = Lists.newArrayList result
        result.close()
        assertThat resultList, contains(
                hasProperty('id', equalTo(-1101L))
        )
    }

    @Test
    void testPropertiesConstraintWithType() {
        IterableResult<BioMarker> result = bioMarkerResourceService.retrieveBioMarkers([
                bioMarkerResourceService.createConstraint(BioMarkerConstraint.PROPERTIES_CONSTRAINT, type: 'PROTEIN')
        ])

        def resultList = Lists.newArrayList result
        result.close()
        assertThat resultList, hasSize(5)
    }

    @Test
    void testPropertiesConstraintWithListOfIds() {
        IterableResult<BioMarker> result = bioMarkerResourceService.retrieveBioMarkers([
                bioMarkerResourceService.createConstraint(BioMarkerConstraint.PROPERTIES_CONSTRAINT, id: [-1101L, -1102L])
        ])

        def resultList = Lists.newArrayList result
        result.close()
        assertThat resultList, containsInAnyOrder(
                hasProperty('id', equalTo(-1101L)),
                hasProperty('id', equalTo(-1102L))
        )
    }

    @Test(expected = InvalidArgumentsException)
    void testCorrelatedBioMarkersConstraintThrowExcOnNoParams() {
        IterableResult<BioMarker> result = bioMarkerResourceService.retrieveBioMarkers([
                bioMarkerResourceService.createConstraint([:], BioMarkerConstraint.CORRELATED_BIO_MARKERS_CONSTRAINT)
        ])

        def resultList = Lists.newArrayList result
        result.close()
        assertThat resultList, hasSize(19)
    }

    @Test
    void testCorrelatedBioMarkersConstraintBySeveralDiffProperties() {
        IterableResult<BioMarker> result = bioMarkerResourceService.retrieveBioMarkers([
                bioMarkerResourceService.createConstraint(BioMarkerConstraint.CORRELATED_BIO_MARKERS_CONSTRAINT,
                        correlatedBioMarkerProperties: [ name: 'AURKA', type: 'GENE' ])
        ])

        def resultList = Lists.newArrayList result
        result.close()
        assertThat resultList, contains(
                hasProperty('name', equalTo('Adiponectin'))
        )
    }

    @Test
    void testCorrelatedBioMarkersConstraintByListOfValues() {
        IterableResult<BioMarker> result = bioMarkerResourceService.retrieveBioMarkers([
                bioMarkerResourceService.createConstraint(BioMarkerConstraint.CORRELATED_BIO_MARKERS_CONSTRAINT,
                        correlatedBioMarkerProperties: [ name: ['AURKA', 'SLC14A2', 'ADIRF'] ])
        ])

        def resultList = Lists.newArrayList result
        result.close()
        assertThat resultList, containsInAnyOrder(
                hasProperty('externalId', equalTo('Q15848')),
                hasProperty('externalId', equalTo('Q15849')),
                hasProperty('externalId', equalTo('Q15847'))
        )
    }

    @Test
    void testCorrelatedBioMarkersConstraintByCorrelationName() {
        IterableResult<BioMarker> result = bioMarkerResourceService.retrieveBioMarkers([
                bioMarkerResourceService.createConstraint(BioMarkerConstraint.CORRELATED_BIO_MARKERS_CONSTRAINT,
                        correlationName: 'GENE TO PROTEIN', correlatedBioMarkerProperties: [ type: 'GENE' ])
        ])

        def resultList = Lists.newArrayList result
        result.close()
        assertThat resultList, containsInAnyOrder(
                hasProperty('externalId', equalTo('Q15848')),
                hasProperty('externalId', equalTo('Q15849')),
                hasProperty('externalId', equalTo('Q15847'))
        )
    }

    @Test
    void testSeveralConstraints() {
        IterableResult<BioMarker> result = bioMarkerResourceService.retrieveBioMarkers([
                bioMarkerResourceService.createConstraint(BioMarkerConstraint.PROPERTIES_CONSTRAINT,
                        name: ['AURKA', 'SLC14A2', 'BOGUSCPOCORREL', 'BOGUSCPO']),
                bioMarkerResourceService.createConstraint(BioMarkerConstraint.CORRELATED_BIO_MARKERS_CONSTRAINT,
                        correlatedBioMarkerProperties: [ type: 'PROTEIN' ])
        ])

        def resultList = Lists.newArrayList result
        result.close()
        assertThat resultList, contains(
                hasProperty('name', equalTo('BOGUSCPO'))
        )
    }

    @Test
    void testAvailableTypes() {
        def result = bioMarkerResourceService.availableTypes()
        def types = bioMarkerTestData.allBioMarkers*.type.unique()
        assertThat result, containsInAnyOrder(*types)
        //containsInAnyOrder("MIRNA", "PATHWAY", "PROTEIN", "GENE", "METABOLITE")
        result.close()
    }

    @Test
    void testAvailableOrganisms() {
        def result = bioMarkerResourceService.availableOrganisms()
        def organisms = bioMarkerTestData.allBioMarkers*.organism.unique()
        assertThat result, containsInAnyOrder(*organisms)
        result.close()
    }
}
