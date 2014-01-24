package org.transmartproject.db.dataquery.highdim.metabolite

import com.google.common.collect.Lists
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.dataquery.highdim.SampleBioMarkerTestData

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.closeTo
import static org.hamcrest.Matchers.contains
import static org.hamcrest.Matchers.hasProperty
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.isA

class MetaboliteEndToEndRetrievalTest {

    private static Double DELTA = 0.000005

    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource metaboliteResource

    @Lazy AssayConstraint trialConstraint = metaboliteResource.createAssayConstraint(
            AssayConstraint.TRIAL_NAME_CONSTRAINT,
            name: MetaboliteTestData.TRIAL_NAME)

    @Lazy Projection projection = metaboliteResource.createProjection([:],
            org.transmartproject.core.dataquery.highdim.projections.Projection.ZSCORE_PROJECTION)

    TabularResult<AssayColumn, MetaboliteDataRow> result

    MetaboliteTestData testData = new MetaboliteTestData()

    @Before
    void setup() {
        testData.saveAll()
        metaboliteResource = highDimensionResourceService.getSubResourceForType('metabolite')
    }

    @After
    void tearDown() {
        result?.close()
    }

    @Test
    void fetchAllDataTest() {
        result = metaboliteResource.retrieveData([trialConstraint], [], projection)

        List rows = Lists.newArrayList result.rows

        assertThat rows, contains(
                testData.annotations.sort { it.id }.
                collect { annotation ->
                    allOf(
                            isA(MetaboliteDataRow),
                            hasProperty('label', is(annotation.biochemicalName)),
                            hasProperty('bioMarker', is(annotation.hmdbId)),
                            contains(
                                    testData.data.findAll { it.annotation == annotation}.
                                    sort { it.assay.id }.
                                    collect { closeTo(it.zscore as Double, DELTA) }
                    ))
                }
        )
    }

    @Test
    void searchWithHmdbId() {
        def hmdbid = 'HMDB30537'
        def dataConstraint = metaboliteResource.createDataConstraint(
                'metabolites',
                names: [hmdbid])

        result = metaboliteResource.retrieveData(
                [ trialConstraint ], [ dataConstraint ], projection)

        assertThat Lists.newArrayList(result), contains(allOf(
                isA(MetaboliteDataRow),
                hasProperty('bioMarker', is(hmdbid)),
                contains(
                        testData.data.findAll { it.annotation.hmdbId == hmdbid }.
                        sort { it.assay.id }.
                        collect { closeTo(it.zscore as Double, DELTA) }
                )
        ))
    }

    @Test
    void searchWithSubPathway() {
        SampleBioMarkerTestData biomarkerTestData = new SampleBioMarkerTestData()

        //BIOMARKER_ID -> subpathway
        def dataConstraint = metaboliteResource.createDataConstraint(
                DataConstraint.PATHWAYS_CONSTRAINT,
                names: [ "To godliness (Harm wrote this)" ])

        result = metaboliteResource.retrieveData(
                [ trialConstraint ], [ dataConstraint ], projection)
        println(result)
    }

    @Test
    void searchWithSuperPathway() {

    }
}
