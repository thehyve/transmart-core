package org.transmartproject.db.dataquery.highdim.protein

import com.google.common.collect.Lists
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.test.Matchers.hasSameInterfaceProperties

class ProteinEndToEndRetrievalTests {

    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource proteinResource

    AssayConstraint trialConstraint

    Projection projection

    TabularResult<AssayColumn, ProteinDataRow> result

    ProteinTestData testData = new ProteinTestData()

    static final Double DELTA = 0.00005

    String ureaTransporterPeptide,
           adiponectinPeptide,
           adipogenesisFactorPeptide

    @Before
    void setUp() {
        testData.saveAll()

        proteinResource = highDimensionResourceService.
                getSubResourceForType 'protein'

        trialConstraint = proteinResource.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: ProteinTestData.TRIAL_NAME)

        projection = proteinResource.createProjection([:],
                Projection.ZSCORE_PROJECTION)

        ureaTransporterPeptide = testData.annotations[-1].peptide
        adiponectinPeptide = testData.annotations[-2].peptide
        adipogenesisFactorPeptide = testData.annotations[-3].peptide
    }

    @After
    void tearDown() {
        result?.close()
    }

    @Test
    void basicTest() {
        result = proteinResource.retrieveData([trialConstraint], [], projection)

        Iterable<AssayColumn> assays = result.indicesList
        assertThat assays, contains(
                hasSameInterfaceProperties(Assay, testData.assays[1]),
                hasSameInterfaceProperties(Assay, testData.assays[0]),)

        assertThat assays, hasItem(
                hasProperty('label', is(testData.assays[-1].sampleCode)))

        assertThat result, allOf(
                hasProperty('columnsDimensionLabel', is('Sample codes')),
                hasProperty('rowsDimensionLabel',    is('Proteins')),
        )

        List rows = Lists.newArrayList result.rows

        assertThat rows, allOf(
                contains(
                        allOf(
                                hasProperty('label', is(ureaTransporterPeptide)),
                                hasProperty('bioMarker', is("PVR_HUMAN3")),
                                hasProperty('peptide', is(testData.annotations[-1].peptide))
                        ),
                        hasProperty('label', is(adiponectinPeptide)),
                        hasProperty('label', is(adipogenesisFactorPeptide))))
    }

    @Test
    void testSearchByProtein() {
        def dataConstraint = proteinResource.createDataConstraint(
                DataConstraint.PROTEINS_CONSTRAINT,
                names: [ 'Urea transporter 2' ])

        result = proteinResource.retrieveData(
                [ trialConstraint ], [ dataConstraint ], projection)

        /* the result is iterable */
        assertThat result, contains(allOf(
                hasProperty('label', equalTo(ureaTransporterPeptide)),
                contains( /* the rows are iterable */
                        closeTo(testData.data[5].zscore as Double, DELTA),
                        closeTo(testData.data[4].zscore as Double, DELTA))))
    }

    def getDataMatcherForAnnotation(DeProteinAnnotation annotation,
                                       String property) {
        contains testData.data.
                findAll { it.annotation == annotation }.
                sort    { it.assay.id }. // data is sorted by assay id
                collect { closeTo it."$property" as Double, DELTA }
    }

    @Test
    void testLogIntensityProjection() {
        def logIntensityProjection = proteinResource.createProjection(
                [:], Projection.LOG_INTENSITY_PROJECTION)

        result = proteinResource.retrieveData(
                [ trialConstraint ], [], logIntensityProjection)

        def resultList = Lists.newArrayList result

        println resultList
        assertThat resultList, containsInAnyOrder(
                testData.annotations.collect {
                    getDataMatcherForAnnotation it, 'logIntensity'
                })
    }

    @Test
    void testDefaultRealProjection() {
        def defaultRealProjection = proteinResource.createProjection(
                [:], Projection.DEFAULT_REAL_PROJECTION)

        result = proteinResource.retrieveData(
                [ trialConstraint ], [], defaultRealProjection)

        assertThat result, hasItem(allOf(
                hasProperty('label', is(ureaTransporterPeptide)),
                contains(
                        closeTo(testData.data[5].intensity as Double, DELTA),
                        closeTo(testData.data[4].intensity as Double, DELTA))))
    }

    @Test
    void testSearchByProteinExternalIds() {
        def dataConstraint = proteinResource.createDataConstraint(
                DataConstraint.PROTEINS_CONSTRAINT,
                ids: testData.proteins.findAll {
                    it.name == 'Adiponectin' || it.name == 'Urea transporter 2'
                }*.primaryExternalId)

        result = proteinResource.retrieveData(
                [ trialConstraint ], [ dataConstraint ], projection)

        assertThat result, contains(
                hasProperty('label', is(ureaTransporterPeptide)),
                hasProperty('label', is(adiponectinPeptide)))
    }

    @Test
    void testSearchByGenes() {
        def dataConstraint = proteinResource.createDataConstraint(
                DataConstraint.GENES_CONSTRAINT,
                names: [ 'AURKA' ])
        // in our test data, gene AURKA is correlated with Adiponectin

        result = proteinResource.retrieveData(
                [ trialConstraint ], [ dataConstraint ], projection)

        assertThat result, contains(
                hasProperty('label', is(adiponectinPeptide)))
    }

    @Test
    void testSearchByPathways() {
        def dataConstraint = proteinResource.createDataConstraint(
                DataConstraint.PATHWAYS_CONSTRAINT,
                names: [ 'FOOPATHWAY' ])
        // in our test data, pathway FOOPATHWAY is correlated with Adiponectin

        result = proteinResource.retrieveData(
                [ trialConstraint ], [ dataConstraint ], projection)
        //TODO It returns empty result. It would be nice to investigate db state at this point
        assertThat Lists.newArrayList(result), contains(
                hasProperty('label', is(adiponectinPeptide)))
    }

}
