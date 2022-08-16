package org.transmartproject.db.dataquery.highdim

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.EmptySetException
import org.transmartproject.db.dataquery.highdim.mrna.MrnaTestData
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static org.transmartproject.db.test.Matchers.hasSameInterfaceProperties
import static spock.util.matcher.HamcrestSupport.that

@Integration
@Rollback
class GeneralHighDimensionDataTypeResourceSpec extends Specification {

    HighDimensionResource highDimensionResourceService

    // use mrna data type for these generic tests instead of mocking a data type
    HighDimensionDataTypeResource mrnaResource

    Closeable dataQueryResult

    MrnaTestData testData = new MrnaTestData()

    AssayConstraint trialNameConstraint

    void setupData() {
        testData.saveAll()

        mrnaResource = highDimensionResourceService.getSubResourceForType 'mrna'
        assert mrnaResource != null

        trialNameConstraint = mrnaResource.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: MrnaTestData.TRIAL_NAME)
    }

    void cleanup() {
        dataQueryResult?.close()
    }

    void testEmptyAssayConstraintList() {
        setupData()
        // this is permitted -- all the assays for the respective marker type
        // are returned

        List dataConstraints = []
        def projection = mrnaResource.createProjection [:], Projection.ZSCORE_PROJECTION

        dataQueryResult = mrnaResource.retrieveData(
                [], dataConstraints, projection)

        expect:
        that(dataQueryResult.indicesList,
                contains(
                        testData.assays.sort { it.id }.collect {
                            hasSameInterfaceProperties(Assay, it)
                        }))
    }

    void testUnsatisfiedAssayConstraints() {
        setupData()
        // if no assay constraint is found, should throw
        List dataConstraints = []
        List assayConstraints = [
                mrnaResource.createAssayConstraint(
                        AssayConstraint.TRIAL_NAME_CONSTRAINT,
                        name: 'nonexistent_trial')
        ]
        def projection =
                mrnaResource.createProjection [:], Projection.ZSCORE_PROJECTION


        when:
        dataQueryResult = mrnaResource.retrieveData(
                assayConstraints, dataConstraints, projection)
        then:
        thrown(EmptySetException)
    }

    void testUnsatisfiedDataConstraints() {
        setupData()
        // if assays are found, but no data is, should not throw
        List dataConstraints = [mrnaResource.createDataConstraint(
                DataConstraint.GENES_CONSTRAINT,
                names: ['AURKA'])]
        def projection =
                mrnaResource.createProjection [:], Projection.ZSCORE_PROJECTION

        when:
        dataQueryResult = mrnaResource.retrieveData(
                [trialNameConstraint], dataConstraints, projection)

        then:
        dataQueryResult.indicesList.size() == testData.assays.size()
        that(dataQueryResult, iterableWithSize(0))
    }

}
