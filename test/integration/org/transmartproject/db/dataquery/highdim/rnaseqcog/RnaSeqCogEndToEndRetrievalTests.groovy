package org.transmartproject.db.dataquery.highdim.rnaseqcog

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
import org.transmartproject.core.dataquery.highdim.projections.Projection

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.test.Matchers.hasSameInterfaceProperties

class RnaSeqCogEndToEndRetrievalTests {

    private static final double DELTA = 0.0001
    TabularResult<AssayColumn, RnaSeqCogDataRow> result

    RnaSeqCogTestData testData = new RnaSeqCogTestData()

    HighDimensionDataTypeResource<RnaSeqCogDataRow> rnaSeqCogResource

    HighDimensionResource highDimensionResourceService

    Projection projection

    AssayConstraint trialNameConstraint

    @Before
    void setUp() {
        testData.saveAll()

        rnaSeqCogResource = highDimensionResourceService.
                getSubResourceForType('rnaseq_cog')

        projection = rnaSeqCogResource.createProjection(
                [:], Projection.ZSCORE_PROJECTION)

        trialNameConstraint = rnaSeqCogResource.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: RnaSeqCogTestData.TRIAL_NAME)
    }

    @After
    void after() {
        result?.close()
    }

    @Test
    void basicTest() {
        result = rnaSeqCogResource.retrieveData([ trialNameConstraint ],
                [], projection)

        assertThat result, allOf(
                hasProperty('columnsDimensionLabel', is('Sample codes')),
                hasProperty('rowsDimensionLabel',    is('Transcripts')),
                hasProperty('indicesList', contains(
                        testData.assays.reverse().collect { Assay it ->
                            hasSameInterfaceProperties(Assay, it)
                        }.collect { is it })))

        def rows = Lists.newArrayList result

        assertThat(rows, contains(
                contains(testData.data[-1..-2]*.zscore.collect { Double it -> closeTo it, DELTA }),
                contains(testData.data[-3..-4]*.zscore.collect { Double it -> closeTo it, DELTA }),
                contains(testData.data[-5..-6]*.zscore.collect { Double it -> closeTo it, DELTA })))
    }

    @Test
    void testDataRowsProperties() {
        result = rnaSeqCogResource.retrieveData([ trialNameConstraint ],
                [], projection)

        assertThat Lists.newArrayList(result), contains(
                testData.annotations.reverse().collect { DeRnaseqAnnotation annotation ->
                    allOf(
                            hasProperty('label',     is(annotation.transcriptId)),
                            hasProperty('bioMarker', is(annotation.geneSymbol)))
                }
        )
    }

    @Test
    void testDefaultRealProjection() {
        result = rnaSeqCogResource.retrieveData([ trialNameConstraint ], [],
                rnaSeqCogResource.createProjection([:], Projection.DEFAULT_REAL_PROJECTION))

        assertThat Lists.newArrayList(result), hasItem(allOf(
                hasProperty('label', is(testData.data[-1].annotation.transcriptId)) /* VNN3 */,
                contains(testData.data[-1..-2]*.rawIntensity.collect { Double it -> closeTo it, DELTA })
        ))
    }

}
