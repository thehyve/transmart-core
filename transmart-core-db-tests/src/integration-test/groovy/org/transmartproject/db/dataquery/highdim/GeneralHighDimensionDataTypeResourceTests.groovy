package org.transmartproject.db.dataquery.highdim

import grails.test.mixin.TestMixin
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.EmptySetException
import org.transmartproject.db.dataquery.highdim.mrna.MrnaTestData
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.db.test.Matchers.hasSameInterfaceProperties

@TestMixin(RuleBasedIntegrationTestMixin)
class GeneralHighDimensionDataTypeResourceTests {

    HighDimensionResource highDimensionResourceService

    // use mrna data type for these generic tests instead of mocking a data type
    HighDimensionDataTypeResource mrnaResource

    Closeable dataQueryResult

    MrnaTestData testData = new MrnaTestData()

    AssayConstraint trialNameConstraint

    @Before
    void setUp() {
        testData.saveAll()

        mrnaResource = highDimensionResourceService.getSubResourceForType 'mrna'
        assertThat mrnaResource, is(notNullValue())

        trialNameConstraint = mrnaResource.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: MrnaTestData.TRIAL_NAME)
    }

    @After
    void after() {
        dataQueryResult?.close()
    }

    @Test
    void testEmptyAssayConstraintList() {
        // this is permitted -- all the assays for the respective marker type
        // are returned

        List dataConstraints = []
        def projection = mrnaResource.createProjection [:], Projection.ZSCORE_PROJECTION

        dataQueryResult = mrnaResource.retrieveData(
                [], dataConstraints, projection)

        assertThat dataQueryResult.indicesList, contains(
                testData.assays.sort { it.id }.collect {
                    hasSameInterfaceProperties(Assay, it)
                })
    }

    @Test
    void testUnsatisfiedAssayConstraints() {
        // if no assay constraint is found, should throw
        List dataConstraints = []
        List assayConstraints = [
                mrnaResource.createAssayConstraint(
                        AssayConstraint.TRIAL_NAME_CONSTRAINT,
                        name: 'nonexistent_trial')
        ]
        def projection =
                mrnaResource.createProjection [:], Projection.ZSCORE_PROJECTION

        shouldFail EmptySetException, {
            dataQueryResult = mrnaResource.retrieveData(
                    assayConstraints, dataConstraints, projection)
        }
    }

    @Test
    void testUnsatisfiedDataConstraints() {
        // if assays are found, but no data is, should not throw
        List dataConstraints = [mrnaResource.createDataConstraint(
                DataConstraint.GENES_CONSTRAINT,
                names: ['AURKA'])]
        def projection =
                mrnaResource.createProjection [:], Projection.ZSCORE_PROJECTION

        dataQueryResult = mrnaResource.retrieveData(
                    [trialNameConstraint], dataConstraints, projection)

        assertThat dataQueryResult, allOf(
                hasProperty('indicesList', hasSize(testData.assays.size())),
                iterableWithSize(0))
    }

}
