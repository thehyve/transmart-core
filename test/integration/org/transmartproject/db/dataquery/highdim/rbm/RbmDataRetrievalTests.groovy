package org.transmartproject.db.dataquery.highdim.rbm

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

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class RbmDataRetrievalTests {

    RbmTestData testData = new RbmTestData()

    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource rbmResource

    AssayConstraint trialNameConstraint

    Projection projection

    TabularResult result

    double delta = 0.0001

    @Test
    void testRetrievalByTrialNameAssayConstraint() {
        result = rbmResource.retrieveData([trialNameConstraint], [], projection)

        assertThat result, allOf(
                hasProperty('columnsDimensionLabel', equalTo('Sample codes')),
                hasProperty('rowsDimensionLabel', equalTo('Antigenes')),
        )

        def resultList = Lists.newArrayList result

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
                        contains(
                                closeTo(testData.rbmData[-1].zscore as Double, delta),
                                closeTo(testData.rbmData[-2].zscore as Double, delta)),
                        contains(
                                closeTo(testData.rbmData[-3].zscore as Double, delta),
                                closeTo(testData.rbmData[-4].zscore as Double, delta)),
                        contains(
                                closeTo(testData.rbmData[-5].zscore as Double, delta),
                                closeTo(testData.rbmData[-6].zscore as Double, delta)),
                ),
                everyItem(
                        hasProperty('assayIndexMap', allOf(
                                isA(Map),
                                hasEntry(
                                        hasProperty('id', equalTo(-402L)), /* key */
                                        equalTo(0), /* value */
                                ),
                                hasEntry(
                                        hasProperty('id', equalTo(-401L)),
                                        equalTo(1),
                                ),
                        ))
                )
        )
    }

    @Test
    void testDefaultRealProjection() {
        result = rbmResource.retrieveData([trialNameConstraint], [],
            rbmResource.createProjection([:], Projection.DEFAULT_REAL_PROJECTION))

        def resultList = Lists.newArrayList result

        assertThat resultList, hasItem(allOf(
                hasProperty('label', is('Antigene1')),
                contains(
                        closeTo(testData.rbmData[1].value as Double, delta),
                        closeTo(testData.rbmData[0].value as Double, delta))))
    }

    @Test
    void testRetrievalByUniProtNamesDataConstraint() {
        def proteinDataConstraint = rbmResource.createDataConstraint(
                [names: ['Adiponectin']],
                DataConstraint.PROTEINS_CONSTRAINT
        )

        result = rbmResource.retrieveData([trialNameConstraint], [proteinDataConstraint], projection)

        def resultList = Lists.newArrayList result

        assertThat resultList, allOf(
                everyItem(
                        allOf(
                                contains(
                                        closeTo(testData.rbmData[-5].zscore as Double, delta),
                                        closeTo(testData.rbmData[-6].zscore as Double, delta)))),
                contains(
                        allOf(
                                hasProperty('bioMarker', equalTo('Q15848')),
                                hasProperty('label', equalTo('Antigene1')))))
    }

    @Test
    void testRetrievalByUniProtIdsDataConstraint() {
        def proteinDataConstraint = rbmResource.createDataConstraint(
                [ids: ['Q15848']],
                DataConstraint.PROTEINS_CONSTRAINT
        )

        result = rbmResource.retrieveData([trialNameConstraint], [proteinDataConstraint], projection)

        def resultList = Lists.newArrayList result

        assertThat resultList, allOf(
                hasSize(1),
                everyItem(
                        hasProperty('data', allOf(
                                hasSize(2),
                                contains(
                                        closeTo(testData.rbmData[-5].zscore as Double, delta),
                                        closeTo(testData.rbmData[-6].zscore as Double, delta),
                                ))
                        )
                ),
                contains(hasProperty('label', equalTo('Antigene1')))
        )
    }

    @Test
    void testRetrievalByGeneNamesDataConstraint() {
        def geneDataConstraint = rbmResource.createDataConstraint(
                [names: ['SLC14A2']],
                DataConstraint.GENES_CONSTRAINT
        )

        result = rbmResource.retrieveData([trialNameConstraint], [geneDataConstraint], projection)

        def resultList = Lists.newArrayList result

        assertThat resultList, contains(
                allOf(
                        hasProperty('label', equalTo('Antigene2')),
                        contains(
                                closeTo(testData.rbmData[-3].zscore as Double, delta),
                                closeTo(testData.rbmData[-4].zscore as Double, delta))))
    }

    @Test
    void testRetrievalByGeneSkIdsDataConstraint() {
        def skId = testData.searchKeywords.find({ it.keyword == 'SLC14A2' }).id
        def geneDataConstraint = rbmResource.createDataConstraint(
                [keyword_ids: [skId]],
                DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT
        )

        result = rbmResource.retrieveData([trialNameConstraint], [geneDataConstraint], projection)

        def resultList = Lists.newArrayList result

        assertThat resultList, allOf(
                everyItem(
                        hasProperty('data',
                                contains(
                                        closeTo(testData.rbmData[-3].zscore as Double, delta),
                                        closeTo(testData.rbmData[-4].zscore as Double, delta)))),
                contains(hasProperty('label', equalTo('Antigene2'))))
    }

    @Test
    void testRetrieDataRowThatMapsToMultipleProteins() {

        def proteinDataConstraint = rbmResource.createDataConstraint(
                [ids: ['Q15847', 'Q15850']],
                DataConstraint.PROTEINS_CONSTRAINT
        )

        result = rbmResource.retrieveData([trialNameConstraint], [proteinDataConstraint], projection)

        def resultList = Lists.newArrayList result

        assertThat resultList, allOf(
                hasSize(1),
                hasItem(
                        allOf(
                                hasProperty('bioMarker', equalTo('Q15850/Q15847')),
                                hasProperty('label', equalTo('Antigene3'))
                        )
                )
        )
    }

    @Test
    void testConstraintAvailability() {
        assertThat rbmResource.supportedAssayConstraints, containsInAnyOrder(
                AssayConstraint.ONTOLOGY_TERM_CONSTRAINT,
                AssayConstraint.PATIENT_SET_CONSTRAINT,
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                AssayConstraint.ASSAY_ID_LIST_CONSTRAINT,
                AssayConstraint.DISJUNCTION_CONSTRAINT)
        assertThat rbmResource.supportedDataConstraints, hasItems(
                DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT,
                DataConstraint.DISJUNCTION_CONSTRAINT,
                DataConstraint.GENES_CONSTRAINT,
                DataConstraint.PROTEINS_CONSTRAINT,
                /* also others that may be added by registering new associations */
        )
        assertThat rbmResource.supportedProjections, containsInAnyOrder(
                Projection.DEFAULT_REAL_PROJECTION,
                Projection.ZSCORE_PROJECTION)
    }

    @Before
    void setUp() {
        testData.saveAll()
        rbmResource = highDimensionResourceService.getSubResourceForType 'rbm'

        trialNameConstraint = rbmResource.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: RbmTestData.TRIAL_NAME,
        )
        projection = rbmResource.createProjection [:], Projection.ZSCORE_PROJECTION
    }

    @After
    void tearDown() {
        result?.close()
    }

}
