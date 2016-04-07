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

package org.transmartproject.db.dataquery.highdim.mrna

import com.google.common.collect.Lists
import grails.test.mixin.TestMixin
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Created by glopes on 11/18/13.
 *
 */
@TestMixin(RuleBasedIntegrationTestMixin)
class MrnaEndToEndRetrievalTests {

    private static final double DELTA = 0.0001
    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource mrnaResource

    Closeable dataQueryResult

    MrnaTestData testData = new MrnaTestData()

    AssayConstraint trialNameConstraint

    @Before
    void setUp() {
        testData.saveAll()

        mrnaResource = highDimensionResourceService.getSubResourceForType 'mrna'
        assertThat mrnaResource, is(notNullValue())

        trialNameConstraint = mrnaResource.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: MrnaTestData.TRIAL_NAME)
    }

    @After
    void after() {
        dataQueryResult?.close()
    }

    @Test
    void basicTest() {
        List dataConstraints = []
        def projection = mrnaResource.createProjection [:], Projection.ZSCORE_PROJECTION

        dataQueryResult = mrnaResource.retrieveData(
                [ trialNameConstraint ], dataConstraints, projection)

        def resultList = Lists.newArrayList dataQueryResult

        /* more extensive assertions in MrnaDataRetrievalTests */
        assertThat resultList, allOf(
                hasSize(3),
                everyItem(
                        hasProperty('data',
                                allOf(
                                        hasSize(2),
                                        everyItem(isA(Double))
                                )
                        )
                ),
                contains(
                        hasProperty('data', contains(
                                closeTo(testData.microarrayData[-1].zscore as Double, DELTA),
                                closeTo(testData.microarrayData[-2].zscore as Double, DELTA),
                        )),
                        hasProperty('data', contains(
                                closeTo(testData.microarrayData[-3].zscore as Double, DELTA),
                                closeTo(testData.microarrayData[-4].zscore as Double, DELTA),
                        )),
                        hasProperty('data', contains(
                                closeTo(testData.microarrayData[-5].zscore as Double, DELTA),
                                closeTo(testData.microarrayData[-6].zscore as Double, DELTA),
                        )),
                )
        )
    }

    @Test
    void testWithGeneConstraint() {
        List dataConstraints = [
                mrnaResource.createDataConstraint([keyword_ids: [testData.searchKeywords.
                        find({ it.keyword == 'BOGUSRQCD1' }).id]],
                        DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT
                )
        ]
        def projection = mrnaResource.createProjection [:], Projection.ZSCORE_PROJECTION

        dataQueryResult = mrnaResource.retrieveData(
                [ trialNameConstraint ], dataConstraints, projection)

        def resultList = Lists.newArrayList dataQueryResult

        assertThat resultList, allOf(
                hasSize(1),
                everyItem(hasProperty('data', hasSize(2))),
                contains(hasProperty('bioMarker', equalTo('BOGUSRQCD1')))
        )
    }

    @Test
    void testLogIntensityProjection() {
        def logIntensityProjection = mrnaResource.createProjection(
                [:], Projection.LOG_INTENSITY_PROJECTION)

        dataQueryResult = mrnaResource.retrieveData(
                [ trialNameConstraint ], [], logIntensityProjection)

        def resultList = Lists.newArrayList dataQueryResult

        assertThat(
                resultList.collect { it.data }.flatten(),
                containsInAnyOrder(testData.microarrayData.collect { closeTo(it.logIntensity as Double, DELTA) })
        )
    }

    @Test
    void testWithDefaultRealProjection() {
        List dataConstraints = [
                mrnaResource.createDataConstraint([keyword_ids: [testData.searchKeywords.
                        find({ it.keyword == 'BOGUSCPO' }).id]],
                        DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT
                )
        ]
        def projection = mrnaResource.createProjection [:], Projection.DEFAULT_REAL_PROJECTION

        dataQueryResult = mrnaResource.retrieveData(
                [ trialNameConstraint ], dataConstraints, projection)

        assertThat dataQueryResult, contains(
                contains(
                        closeTo(testData.microarrayData[1].rawIntensity as Double, DELTA),
                        closeTo(testData.microarrayData[0].rawIntensity as Double, DELTA),
                )
        )
    }

    // not really retrieval
    @Test
    void testConstraintAvailability() {
        assertThat mrnaResource.supportedAssayConstraints, containsInAnyOrder(
                AssayConstraint.ONTOLOGY_TERM_CONSTRAINT,
                AssayConstraint.PATIENT_SET_CONSTRAINT,
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                AssayConstraint.ASSAY_ID_LIST_CONSTRAINT,
                AssayConstraint.PATIENT_ID_LIST_CONSTRAINT,
                AssayConstraint.DISJUNCTION_CONSTRAINT)
        assertThat mrnaResource.supportedDataConstraints, hasItems(
                DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT,
                DataConstraint.DISJUNCTION_CONSTRAINT,
                DataConstraint.GENES_CONSTRAINT,
                DataConstraint.GENE_SIGNATURES_CONSTRAINT,
                DataConstraint.PATHWAYS_CONSTRAINT,
                DataConstraint.PROTEINS_CONSTRAINT,
                /* also others that may be added by registering new associations */
        )
        assertThat mrnaResource.supportedProjections, containsInAnyOrder(
                Projection.DEFAULT_REAL_PROJECTION,
                Projection.LOG_INTENSITY_PROJECTION,
                Projection.ZSCORE_PROJECTION,
                Projection.ALL_DATA_PROJECTION)
    }


}
