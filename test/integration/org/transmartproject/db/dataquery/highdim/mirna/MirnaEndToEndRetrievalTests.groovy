package org.transmartproject.db.dataquery.highdim.mirna

import com.google.common.collect.Lists
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.search.SearchKeywordCoreDb

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class MirnaEndToEndRetrievalTests {

    MirnaTestData testData = new MirnaTestData()

    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource mirnaResource

    AssayConstraint trialNameConstraint

    Projection projection

    TabularResult result

    @Before
    void setUp() {
        testData.saveAll()
        mirnaResource = highDimensionResourceService.getSubResourceForType 'mirna'

        trialNameConstraint = mirnaResource.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: MirnaTestData.TRIAL_NAME,
        )
        projection = mirnaResource.createProjection [:], 'default_real_projection'
    }

    @After
    void tearDown() {
        result?.close()
    }


    @Test
    void basicTest() {
        def dataConstraints = [
                mirnaResource.createDataConstraint(
                        'mirnas', names: [ 'MIR323B', 'MIR3161' ])
        ]

        result = mirnaResource.retrieveData(
                [ trialNameConstraint ], dataConstraints, projection)

        assertThat result.indicesList, contains(
                hasProperty('label', equalTo('SAMPLE_FOR_-302')),
                hasProperty('label', equalTo('SAMPLE_FOR_-301')),
        )

        assertThat result, allOf(
                hasProperty('rowsDimensionLabel', equalTo('Probes')),
                hasProperty('columnsDimensionLabel', equalTo('Sample codes')),
        )

        List rows = Lists.newArrayList result.rows

        double delta = 0.0001
        assertThat rows, contains(
                allOf(
                        hasProperty('label', equalTo('hsa-mir-323b')),
                        hasProperty('data', contains(
                                closeTo(testData.mirnaData[5].zscore as Double, delta),
                                closeTo(testData.mirnaData[4].zscore as Double, delta),
                        ))
                ),
                allOf(
                        hasProperty('label', equalTo('hsa-mir-3161')),
                        hasProperty('data', contains(
                                closeTo(testData.mirnaData[1].zscore as Double, delta),
                                closeTo(testData.mirnaData[0].zscore as Double, delta),
                        ))
                ),
        )
    }

    @Test
    void testSearchBySearchKeywordIds() {
        def dataConstraints = [
                mirnaResource.createDataConstraint(
                        DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT,
                        keyword_ids: SearchKeywordCoreDb.
                                findAllByKeywordInList([ 'MIR3161' ])*.id)
        ]

        result = mirnaResource.retrieveData(
                [ trialNameConstraint ], dataConstraints, projection)

        assertThat Lists.newArrayList(result.rows), contains(
                        hasProperty('label', equalTo('hsa-mir-3161')))
    }

    @Test
    void testFallbackToDetector() {
        def dataConstraints = []

        result = mirnaResource.retrieveData(
                [ trialNameConstraint ], dataConstraints, projection)

        List rows = Lists.newArrayList result.rows

        assertThat rows, allOf(
                hasSize(3),
                hasItem(
                        hasProperty('label', equalTo(testData.probes[1].detector))
                )
        )
    }


    @Test
    void testBadMirnaConstraints() {
        shouldFail InvalidArgumentsException, {
            mirnaResource.createDataConstraint 'mirnas', names: 'foobar'
        }
        shouldFail InvalidArgumentsException, {
            mirnaResource.createDataConstraint 'mirnas', namezzz: [ 'dfsdf' ]
        }
    }

    @Test
    void testDataProjection() {
        shouldFail InvalidArgumentsException, {
            mirnaResource.createProjection 'default_real_projection', arg: 'value'
        }
    }

}
