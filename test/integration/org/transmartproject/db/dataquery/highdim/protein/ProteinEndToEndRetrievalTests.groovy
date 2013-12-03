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
import org.transmartproject.db.dataquery.highdim.projections.CriteriaProjection

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

    static final Double DELTA = 0.00001

    @Before
    void setUp() {
        testData.saveAll()

        proteinResource = highDimensionResourceService.
                getSubResourceForType 'protein'

        trialConstraint = proteinResource.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: ProteinTestData.TRIAL_NAME)

        projection = proteinResource.createProjection([:],
                CriteriaProjection.DEFAULT_REAL_PROJECTION)
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
                                hasProperty('label', is(
                                        testData.proteins.find {
                                            it.name == 'Urea transporter 2'
                                        }.primaryExternalId)),
                                hasProperty('peptide', is(testData.annotations[-1].peptide))
                        ),
                        hasProperty('label', is(
                                testData.proteins.find {
                                    it.name == 'Adiponectin'
                                }.primaryExternalId)),
                        hasProperty('label', is(
                                testData.proteins.find {
                                    it.name == 'Adipogenesis regulatory factor'
                                }.primaryExternalId))))
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
                hasProperty('label', is(
                        testData.proteins.find {
                            it.name == 'Urea transporter 2'
                        }.primaryExternalId)),
                contains( /* the rows are iterable */
                        closeTo(testData.data[5].intensity as Double, DELTA),
                        closeTo(testData.data[4].intensity as Double, DELTA),)))
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
                hasProperty('label', is(
                        testData.proteins.find {
                            it.name == 'Urea transporter 2'
                        }.primaryExternalId)),
                hasProperty('label', is(
                        testData.proteins.find {
                            it.name == 'Adiponectin'
                        }.primaryExternalId)))
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
                hasProperty('label', is(
                        testData.proteins.find {
                            it.name == 'Adiponectin'
                        }.primaryExternalId)))
    }


}
