/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.dataquery.highdim.protein

import com.google.common.collect.Lists
import grails.test.mixin.TestMixin
import groovy.test.GroovyAssert
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
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.querytool.ConstraintByOmicsValue
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.db.test.Matchers.hasSameInterfaceProperties

@TestMixin(RuleBasedIntegrationTestMixin)
class ProteinEndToEndRetrievalTests {

    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource proteinResource

    AssayConstraint trialConstraint

    Projection projection

    TabularResult<AssayColumn, ProteinDataRow> result

    ProteinTestData testData = new ProteinTestData()

    static final Double DELTA = 0.00005
    private static final String concept_code = 'concept code #1'

    String ureaTransporterPeptide,
           adiponectinPeptide,
           adipogenesisFactorPeptide

    String conceptKey

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

        conceptKey = '\\\\' + testData.concept.tableAccesses[0].tableCode + testData.concept.conceptDimensions[0].conceptPath
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
                }*.externalId)

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

    @Test
    void testSearchAnnotationUniprotNameMulti() {
        def uniprotNames = proteinResource.searchAnnotation(concept_code, 'PVR', 'uniprotName')
        assertThat uniprotNames, allOf(
                hasSize(3),
                contains(
                        equalTo('PVR_HUMAN1'),
                        equalTo('PVR_HUMAN2'),
                        equalTo('PVR_HUMAN3')
                )
        )
    }

    @Test
    void testSearchAnnotationUniprotNameSingle() {
        def uniprotNames = proteinResource.searchAnnotation(concept_code, 'PVR_HUMAN1', 'uniprotName')
        assertThat uniprotNames, allOf(
                hasSize(1),
                contains(equalTo('PVR_HUMAN1'))
        )
    }

    @Test
    void testSearchAnnotationUniprotNameEmpty() {
        def uniprotNames = proteinResource.searchAnnotation(concept_code, 'H', 'uniprotName')
        assertThat uniprotNames, hasSize(0)
    }

    @Test
    void testSearchAnnotationPeptideMulti() {
        def peptides = proteinResource.searchAnnotation(concept_code, 'M', 'peptide')
        assertThat peptides, allOf(
                hasSize(3),
                contains(
                        equalTo('MASKGLQDLK'),
                        equalTo('MLLLGAVLLL'),
                        equalTo('MSDPHSSPLL')
                )
        )
    }

    @Test
    void testSearchAnnotationPeptideSingle() {
        def peptides = proteinResource.searchAnnotation(concept_code, 'MA', 'peptide')
        assertThat peptides, allOf(
                hasSize(1),
                contains(equalTo('MASKGLQDLK'))
        )
    }

    @Test
    void testSearchAnnotationInvalid() {
        GroovyAssert.shouldFail(InvalidArgumentsException.class) {proteinResource.searchAnnotation(concept_code, 'PVR', 'FOO')}
    }

    @Test
    void testLogIntensityConstraint() {
        def constraint = new ConstraintByOmicsValue(
                omicsType: ConstraintByOmicsValue.OmicsType.PROTEOMICS,
                property: 'uniprotName',
                selector: 'PVR_HUMAN1',
                projectionType: Projection.LOG_INTENSITY_PROJECTION,
                operator: 'BETWEEN',
                constraint: '-3:-2'
        )

        def distribution = proteinResource.getDistribution(constraint, conceptKey, null)
        def correctValues = testData.data.findAll {it.annotation.uniprotName == 'PVR_HUMAN1' && -3 <= it.logIntensity && it.logIntensity <= -2}.collectEntries {[it.patient.id, it.logIntensity]}
        assertThat distribution.size(), greaterThanOrEqualTo(1)
        assert distribution.equals(correctValues) // groovy maps are equal if they have same size, keys and values
    }

    @Test
    void testRawIntensityConstraint() {
        def constraint = new ConstraintByOmicsValue(
                omicsType: ConstraintByOmicsValue.OmicsType.PROTEOMICS,
                property: 'uniprotName',
                selector: 'PVR_HUMAN1',
                projectionType: Projection.DEFAULT_REAL_PROJECTION,
                operator: 'BETWEEN',
                constraint: '0.05:0.15'
        )

        def distribution = proteinResource.getDistribution(constraint, conceptKey, null)
        def correctValues = testData.data.findAll {it.annotation.uniprotName == 'PVR_HUMAN1' && 0.05 <= it.intensity && it.intensity <= 0.15}.collectEntries {[it.patient.id, it.intensity]}
        assertThat distribution.size(), greaterThanOrEqualTo(1)
        assert distribution.equals(correctValues) // groovy maps are equal if they have same size, keys and values
    }

    @Test
    void testZScoreConstraint() {
        def constraint = new ConstraintByOmicsValue(
                omicsType: ConstraintByOmicsValue.OmicsType.PROTEOMICS,
                property: 'uniprotName',
                selector: 'PVR_HUMAN1',
                projectionType: Projection.ZSCORE_PROJECTION,
                operator: 'BETWEEN',
                constraint: '-1.5:-1'
        )

        def distribution = proteinResource.getDistribution(constraint, conceptKey, null)
        def correctValues = testData.data.findAll {it.annotation.uniprotName == 'PVR_HUMAN1' && -1.5 <= it.zscore && it.zscore <= -1}.collectEntries {[it.patient.id, it.zscore]}
        assertThat distribution.size(), greaterThanOrEqualTo(1)
        assert distribution.equals(correctValues) // groovy maps are equal if they have same size, keys and values
    }
}
