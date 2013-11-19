package org.transmartproject.db.dataquery.highdim.mrna

import com.google.common.collect.Lists
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Created by glopes on 11/18/13.
 */
class MrnaEndToEndRetrievalTests {

    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource mrnaResource

    Closeable dataQueryResult

    @Before
    void setUp() {
        MrnaTestData.saveAll()

        mrnaResource = highDimensionResourceService.getSubResourceForType 'mrna'

        assertThat mrnaResource, is(notNullValue())
    }

    @After
    void after() {
        dataQueryResult.close()
    }

    @Test
    void basicTest() {
        List assayConstraints = [
                mrnaResource.createAssayConstraint([name: MrnaTestData.TRIAL_NAME],
                        AssayConstraint.TRIAL_NAME_CONSTRAINT
                )
        ]
        List dataConstraints = []
        def projection = mrnaResource.createProjection [:], 'defaultRealProjection'

        dataQueryResult =
            mrnaResource.retrieveData assayConstraints, dataConstraints, projection


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
        )
    }

    @Test
    void testWithGeneConstraint() {
        List assayConstraints = [
                mrnaResource.createAssayConstraint([name: MrnaTestData.TRIAL_NAME],
                        AssayConstraint.TRIAL_NAME_CONSTRAINT
                )
        ]
        List dataConstraints = [
                mrnaResource.createDataConstraint([keyword_ids: [MrnaTestData.searchKeywords.
                        find({ it.keyword == 'BOGUSRQCD1' }).id]],
                        DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT
                )
        ]
        def projection = mrnaResource.createProjection [:], 'defaultRealProjection'

        dataQueryResult =
            mrnaResource.retrieveData assayConstraints, dataConstraints, projection

        def resultList = Lists.newArrayList dataQueryResult

        assertThat resultList, allOf(
                hasSize(1),
                everyItem(hasProperty('data', hasSize(2))),
                contains(hasProperty('geneSymbol', equalTo('BOGUSRQCD1')))
        )
    }


}
