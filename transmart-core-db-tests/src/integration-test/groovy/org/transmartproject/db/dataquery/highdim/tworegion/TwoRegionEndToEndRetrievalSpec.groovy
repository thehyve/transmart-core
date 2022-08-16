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
import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.transmartproject.core.dataquery.ColumnOrderAwareDataRow
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.dataquery.highdim.tworegion.Junction
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that

/**
 * Created by j.hudecek on 17-3-14.
 */

@Integration
@Rollback
class TwoRegionEndToEndRetrievalSpec extends Specification {

    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource resource

    Closeable dataQueryResult

    TwoRegionTestData testData = new TwoRegionTestData()

    AssayConstraint trialNameConstraint

    void setupData() {
        testData.saveAll()

        resource = highDimensionResourceService.getSubResourceForType 'two_region'
        assert resource != null

        trialNameConstraint = resource.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: TwoRegionTestData.TRIAL_NAME)
    }

    void cleanup() {
        dataQueryResult?.close()
    }

    void testWithConstraints() {
        setupData()
        List dataConstraints = [resource.createDataConstraint(
                [chromosome: "1", start: 6, end: 12], DataConstraint.CHROMOSOME_SEGMENT_CONSTRAINT
        )]

        def projection = resource.createProjection [:], Projection.ALL_DATA_PROJECTION

        dataQueryResult = resource.retrieveData(
                [trialNameConstraint], dataConstraints, projection)

        def resultList = Lists.newArrayList(dataQueryResult)

        //resultList[0].data.find({it && it.junctions.size() == 1});
        expect:
        resultList.size() == 1 //only one junctionsrow is returned
        that(resultList[0].junction, allOf(
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
        ))
    }

    void testMultiple() {
        setupData()
        def dataConstraints = [resource.createDataConstraint(
                [chromosome: "3", start: 6, end: 12], DataConstraint.CHROMOSOME_SEGMENT_CONSTRAINT
        )]

        def projection = resource.createProjection [:], Projection.ALL_DATA_PROJECTION

        dataQueryResult = resource.retrieveData(
                [trialNameConstraint], dataConstraints, projection)

        def resultList = Lists.newArrayList(dataQueryResult)

        expect:
        resultList.size() == 4
        that(resultList, hasItem(allOf(
                isA(JunctionRow),
                hasItem(allOf(
                        //junction included in 2 different events
                        isA(Junction),
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
                )))))
        that(resultList, hasItem( // junctionRow
                hasItem(allOf( // junction
                        hasProperty('junctionEvents', hasSize(0)),
                        hasProperty('downChromosome', equalTo('Y'))
                ))))
    }

    void testAssayMappingIsCorrect() {
        setupData()
        def projection = resource.createProjection [:], Projection.ALL_DATA_PROJECTION

        when:
        dataQueryResult = resource.retrieveData([trialNameConstraint], [], projection)
        List<ColumnOrderAwareDataRow<AssayColumn, Junction>> resultList = Lists.newArrayList(dataQueryResult)

        then:
        dataQueryResult.indicesList.size() == testData.assays.size()
        resultList.size() == testData.junctions.size()
        resultList.every { ColumnOrderAwareDataRow<AssayColumn, Junction> row ->
            def foundIndex
            def foundAssayColumn
            dataQueryResult.indicesList.eachWithIndex { AssayColumn assayColumn, int index ->
                if (row[assayColumn] == null) {
                    return
                }

                foundAssayColumn = assayColumn
                foundIndex = index
            }

            foundAssayColumn != null &&
                    foundIndex != null &&
                    row[foundIndex].is(row[foundAssayColumn]) &&
                    row[foundIndex].assay.id == foundAssayColumn.id
        }
    }

}
