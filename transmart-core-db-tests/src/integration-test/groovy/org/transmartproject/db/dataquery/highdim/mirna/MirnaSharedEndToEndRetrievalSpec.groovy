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

package org.transmartproject.db.dataquery.highdim.mirna

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import groovy.util.logging.Slf4j
import spock.lang.Specification

import com.google.common.collect.Lists
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.db.dataquery.highdim.HighDimTestData
import org.transmartproject.db.search.SearchKeywordCoreDb

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.Matchers.*
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.createTestAssays

@Integration
@Rollback
@Slf4j
abstract class MirnaSharedEndToEndRetrievalSpec extends Specification {

    private static final double DELTA = 0.0001

    MirnaTestData testData

    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource mirnaResource

    AssayConstraint trialNameConstraint

    Projection projection

    TabularResult result

    abstract String getTypeName()

    void setup() {
        testData = new MirnaTestData(typeName)
        testData.saveAll()
        mirnaResource = highDimensionResourceService.getSubResourceForType typeName

        trialNameConstraint = mirnaResource.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: MirnaTestData.TRIAL_NAME,
        )
        projection = mirnaResource.createProjection [:], Projection.ZSCORE_PROJECTION
    }

    void cleanup() {
        result?.close()
    }


    void basicTest() {
        def dataConstraints = [
                mirnaResource.createDataConstraint(
                        'mirnas', names: ['MIR323B', 'MIR3161'])
        ]

        when:
        result = mirnaResource.retrieveData(
                [ trialNameConstraint ], dataConstraints, projection)

        then:
        result.indicesList contains(
                hasProperty('label', equalTo('SAMPLE_FOR_-302')),
                hasProperty('label', equalTo('SAMPLE_FOR_-301')),
        )
        result allOf(
                hasProperty('rowsDimensionLabel', equalTo('Probes')),
                hasProperty('columnsDimensionLabel', equalTo('Sample codes')),
        )

        when:
        List rows = Lists.newArrayList result.rows

        then:
        rows contains(
                allOf(
                        hasProperty('label', equalTo('-503')),
                        hasProperty('bioMarker', equalTo(DeQpcrMirnaAnnotation.get(-503).mirnaId)),
                        contains(
                                closeTo(testData.mirnaData[5].zscore as Double, DELTA),
                                closeTo(testData.mirnaData[4].zscore as Double, DELTA))),
                allOf(
                        hasProperty('label', equalTo('-501')),
                        hasProperty('bioMarker', equalTo(DeQpcrMirnaAnnotation.get(-501).mirnaId)),
                        contains(
                                closeTo(testData.mirnaData[1].zscore as Double, DELTA),
                                closeTo(testData.mirnaData[0].zscore as Double, DELTA))))
    }

    void testLogIntensityProjection() {
        def logIntensityProjection = mirnaResource.createProjection(
                [:], Projection.LOG_INTENSITY_PROJECTION)

        result = mirnaResource.retrieveData(
                [ trialNameConstraint ], [], logIntensityProjection)

        def resultList = Lists.newArrayList(result)

        expect:
            resultList.collect { it.data }.flatten()
                containsInAnyOrder(testData.mirnaData.collect { closeTo(it.logIntensity as Double, DELTA) })
    }

    void testDefaultRealProjection() {
        def defaultRealProjection = mirnaResource.createProjection(
                [:], Projection.DEFAULT_REAL_PROJECTION)
        result = mirnaResource.retrieveData([ trialNameConstraint ], [], defaultRealProjection)

        expect: result hasItem(allOf(
                hasProperty('label', equalTo('-503')),
                contains(
                        closeTo(testData.mirnaData[5].rawIntensity as Double, DELTA),
                        closeTo(testData.mirnaData[4].rawIntensity as Double, DELTA))))
    }

    void testSearchBySearchKeywordIds() {
        def dataConstraints = [
                mirnaResource.createDataConstraint(
                        DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT,
                        keyword_ids: SearchKeywordCoreDb.
                                findAllByKeywordInList([ 'MIR3161' ])*.id)
        ]

        result = mirnaResource.retrieveData(
                [ trialNameConstraint ], dataConstraints, projection)

        expect: Lists.newArrayList(result.rows) contains(
                        hasProperty('label', equalTo('-501')))
    }

    void testBadMirnaConstraints() {
        shouldFail InvalidArgumentsException, {
            mirnaResource.createDataConstraint 'mirnas', names: 'foobar'
        }
        shouldFail InvalidArgumentsException, {
            mirnaResource.createDataConstraint 'mirnas', namezzz: [ 'dfsdf' ]
        }
    }

    void testDataProjection() {
        shouldFail InvalidArgumentsException, {
            mirnaResource.createProjection 'default_real_projection', arg: 'value'
        }
    }

    private TabularResult testWithMissingDataAssay(Long baseAssayId) {
        def extraAssays = createTestAssays([ testData.patients[0] ], baseAssayId,
                testData.platform, MirnaTestData.TRIAL_NAME)
        HighDimTestData.save extraAssays

        List assayConstraints = [trialNameConstraint]

        result =
            mirnaResource.retrieveData assayConstraints, [], projection
    }

    void testMissingAssaysNotAllowedFails() {
        testWithMissingDataAssay(-50000L)
        result.allowMissingAssays = false
        shouldFail UnexpectedResultException, {
            result.rows.next()
        }
    }

    void testMissingAssaysAllowedSucceeds() {
        testWithMissingDataAssay(-50000L)
        expect: Lists.newArrayList(result.rows) everyItem(
                hasProperty('data', allOf(
                        hasSize(3), // for the three assays
                        contains(
                                is(nullValue()),
                                is(notNullValue()),
                                is(notNullValue()),
                        )
                ))
        )
    }
}
