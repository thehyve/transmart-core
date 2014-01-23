package org.transmartproject.db.dataquery.highdim.metabolite

import com.google.common.collect.Lists
import org.hibernate.criterion.Projection
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.db.dataquery.highdim.SampleBioMarkerTestData

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.contains
import static org.hamcrest.Matchers.hasProperty
import static org.hamcrest.Matchers.is

class MetaboliteEndToEndRetrievalTest {

    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource metaboliteResource

    AssayConstraint trialConstraint

    Projection projection

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
        def trialConstraint = metaboliteResource.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: MetaboliteTestData.TRIAL_NAME)

        def projection = metaboliteResource.createProjection([:],
                org.transmartproject.core.dataquery.highdim.projections.Projection.ZSCORE_PROJECTION)

        result = metaboliteResource.retrieveData([trialConstraint], [], projection)

        List rows = Lists.newArrayList result.rows

        /* A MetaboliteDataRow (the type of rows[i]) is a construct which aggregates an annotation and data */
        assert rows.size() == testData.annotations.size()

        def row = rows[0]

        assert row.hmdbId == "HMDB00107"
        assert row.biochemicalName == "Galactitol"
        assert row.data.size() == 2
    }

    @Test
    void searchWithHmdbId() {
        def dataConstraint = metaboliteResource.createDataConstraint(
                'metabolites',
                names: [ 'HMDB00107' ])

        result = metaboliteResource.retrieveData(
                [ trialConstraint ], [ dataConstraint ], projection)

        println(result)
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
