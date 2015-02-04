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

package org.transmartproject.db.dataquery.highdim.tworegion

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
 * Created by j.hudecek on 17-3-14.
 */
@TestMixin(RuleBasedIntegrationTestMixin)
class TwoRegionEndToEndRetrievalTests {

    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource resource

    Closeable dataQueryResult

    TwoRegionTestData testData = new TwoRegionTestData()

    AssayConstraint trialNameConstraint

    @Before
    void setUp() {
        testData.saveAll()

        resource = highDimensionResourceService.getSubResourceForType 'two_region'
        assertThat resource, is(notNullValue())

        trialNameConstraint = resource.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: TwoRegionTestData.TRIAL_NAME)
    }

    @After
    void after() {
        dataQueryResult?.close()
    }

    @Test
    void basicTestWithConstraints() {
        List dataConstraints = [resource.createDataConstraint(
                [chromosome: "1", start: 6, end: 12], DataConstraint.CHROMOSOME_SEGMENT_CONSTRAINT
        )]

        def projection = resource.createProjection [:], Projection.ALL_DATA_PROJECTION

        dataQueryResult = resource.retrieveData(
                [trialNameConstraint], dataConstraints, projection)

        def resultList = Lists.newArrayList(dataQueryResult)

        //resultList[0].data.find({it && it.junctions.size() == 1});
        assertThat resultList, hasSize(1) //only one junctionsrow is returned
        assertThat resultList[0].junction, allOf(
                hasProperty('downChromosome', equalTo("1")),
                hasProperty('downPos', equalTo(2L)),
                hasProperty('downEnd', equalTo(10L)),
                hasProperty('isInFrame', equalTo(true)),
                hasProperty('junctionEvents',
                        allOf(
                                hasSize(1),
                                hasItem(
                                        allOf(
                                                hasProperty('event')
                                        ))
                        )
                )
        )
    }

    @Test
    void testMultiple() {
        def dataConstraints = [resource.createDataConstraint(
                [chromosome: "3", start: 6, end: 12], DataConstraint.CHROMOSOME_SEGMENT_CONSTRAINT
        )]

        def projection = resource.createProjection [:], Projection.ALL_DATA_PROJECTION

        dataQueryResult = resource.retrieveData(
                [trialNameConstraint], dataConstraints, projection)

        def resultList = Lists.newArrayList(dataQueryResult)

        assertThat resultList, hasSize(4)
        assertThat resultList, everyItem(hasProperty('data', notNullValue()))
        assertThat resultList, everyItem(hasProperty('assayIndexMap', notNullValue()))
        assertThat resultList, hasItem(hasProperty('junction',
                allOf(
                        //junction included in 2 different events
                        hasProperty('junctionEvents', hasSize(2)),
                        hasProperty('junctionEvents', hasItem(
                                allOf(
                                        hasProperty('pairsSpan', equalTo(11)),
                                        hasProperty('event', hasProperty('cgaType', equalTo("deletion"))),
                                        hasProperty('event', hasProperty('eventGenes', hasSize(0)))
                                )
                        )),
                        hasProperty('junctionEvents', hasItem(
                                allOf(
                                        hasProperty('pairsSpan', equalTo(10)),
                                        hasProperty('event', allOf(
                                                hasProperty('soapClass', equalTo("translocation")),
                                                hasProperty('eventGenes', hasSize(1))
                                        )),
                                        hasProperty('event',
                                                hasProperty('eventGenes', hasItem(
                                                        hasProperty('geneId', equalTo('TP53'))
                                                ))
                                        )
                                ))
                        ),
                        hasProperty('downChromosome', equalTo('X')),
                        hasProperty('downPos', equalTo(2L)),
                        hasProperty('downEnd', equalTo(10L)),
                        hasProperty('upChromosome', equalTo('3')),
                        hasProperty('upPos', equalTo(12L)),
                        hasProperty('upEnd', equalTo(18L)),
                        hasProperty('isInFrame', equalTo(true))
                ))
        )
        assertThat resultList, hasItem(hasProperty('junction',
                //junction included in 0 events
                allOf(
                        hasProperty('junctionEvents', hasSize(0)),
                        hasProperty('downChromosome', equalTo('Y'))
                )
        ))
    }

}
