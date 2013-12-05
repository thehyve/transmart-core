package org.transmartproject.db.dataquery.highdim.mrna

import com.google.common.collect.Lists
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.db.dataquery.highdim.projections.CriteriaProjection

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Created by glopes on 11/18/13.
 */
class MrnaEndToEndRetrievalTests {

    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource mrnaResource

    Closeable dataQueryResult

    MrnaTestData testData = new MrnaTestData()

    @Before
    void setUp() {
        testData.saveAll()

        mrnaResource = highDimensionResourceService.getSubResourceForType 'mrna'

        assertThat mrnaResource, is(notNullValue())
    }

    @After
    void after() {
        dataQueryResult?.close()
    }

    @Test
    void basicTest() {
        List assayConstraints = [
                mrnaResource.createAssayConstraint([name: MrnaTestData.TRIAL_NAME],
                        AssayConstraint.TRIAL_NAME_CONSTRAINT
                )
        ]
        List dataConstraints = []
        def projection = mrnaResource.createProjection [:], 'default_real_projection'

        dataQueryResult =
            mrnaResource.retrieveData assayConstraints, dataConstraints, projection


        def resultList = Lists.newArrayList dataQueryResult

        double delta = 0.0001

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
                                closeTo(testData.microarrayData[-1].zscore as Double, delta),
                                closeTo(testData.microarrayData[-2].zscore as Double, delta),
                        )),
                        hasProperty('data', contains(
                                closeTo(testData.microarrayData[-3].zscore as Double, delta),
                                closeTo(testData.microarrayData[-4].zscore as Double, delta),
                        )),
                        hasProperty('data', contains(
                                closeTo(testData.microarrayData[-5].zscore as Double, delta),
                                closeTo(testData.microarrayData[-6].zscore as Double, delta),
                        )),
                )
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
                mrnaResource.createDataConstraint([keyword_ids: [testData.searchKeywords.
                        find({ it.keyword == 'BOGUSRQCD1' }).id]],
                        DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT
                )
        ]
        def projection = mrnaResource.createProjection [:], 'default_real_projection'

        dataQueryResult =
            mrnaResource.retrieveData assayConstraints, dataConstraints, projection

        def resultList = Lists.newArrayList dataQueryResult

        assertThat resultList, allOf(
                hasSize(1),
                everyItem(hasProperty('data', hasSize(2))),
                contains(hasProperty('bioMarker', equalTo('BOGUSRQCD1')))
        )
    }

    // not really retrieval
    @Test
    void testConstraintAvailability() {
        assertThat mrnaResource.supportedAssayConstraints, containsInAnyOrder(
                AssayConstraint.ONTOLOGY_TERM_CONSTRAINT,
                AssayConstraint.PATIENT_SET_CONSTRAINT,
                AssayConstraint.TRIAL_NAME_CONSTRAINT)
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
                CriteriaProjection.DEFAULT_REAL_PROJECTION)
    }


}
