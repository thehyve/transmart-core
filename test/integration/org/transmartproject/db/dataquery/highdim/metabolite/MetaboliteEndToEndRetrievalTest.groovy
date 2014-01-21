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
    void teardown() {
        result?.close()
    }

    @Test
    void dataPersistenceTest() {
        def trialConstraint = metaboliteResource.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: MetaboliteTestData.TRIAL_NAME)

        def projection = metaboliteResource.createProjection([:],
                org.transmartproject.core.dataquery.highdim.projections.Projection.ZSCORE_PROJECTION)

        result = metaboliteResource.retrieveData([trialConstraint], [], projection)

        List rows = Lists.newArrayList result.rows

        println(rows)
    }
}