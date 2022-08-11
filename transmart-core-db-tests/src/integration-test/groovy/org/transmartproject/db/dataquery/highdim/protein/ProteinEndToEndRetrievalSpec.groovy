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
import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static org.transmartproject.db.test.Matchers.hasSameInterfaceProperties
import static spock.util.matcher.HamcrestSupport.that

@Integration
@Rollback
class ProteinEndToEndRetrievalSpec extends Specification {

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

    void setupData() {
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

    void cleanup() {
        result?.close()
    }

    void basicTest() {
        setupData()
        result = proteinResource.retrieveData([trialConstraint], [], projection)

        when:
        Iterable<AssayColumn> assays = result.indicesList
        then:
        that(assays, contains(
                hasSameInterfaceProperties(Assay, testData.assays[1]),
                hasSameInterfaceProperties(Assay, testData.assays[0]),))
        that(assays, hasItem(
                hasProperty('label', is(testData.assays[-1].sampleCode))))
        result.columnsDimensionLabel == 'Sample codes'
        result.rowsDimensionLabel == 'Proteins'

        when:
        List rows = Lists.newArrayList result.rows

        then:
        that(rows, allOf(
                contains(
                        allOf(
                                hasProperty('label', is(ureaTransporterPeptide)),
                                hasProperty('bioMarker', is("PVR_HUMAN3")),
                                hasProperty('peptide', is(testData.annotations[-1].peptide))
                        ),
                        hasProperty('label', is(adiponectinPeptide)),
                        hasProperty('label', is(adipogenesisFactorPeptide)))))
    }

    void testSearchByProtein() {
        setupData()
        def dataConstraint = proteinResource.createDataConstraint(
                DataConstraint.PROTEINS_CONSTRAINT,
                names: ['Urea transporter 2'])

        result = proteinResource.retrieveData(
                [trialConstraint], [dataConstraint], projection)
        def resultList = Lists.newArrayList result

        /* the result is iterable */
        expect:
        that(resultList, contains(allOf(
                hasProperty('label', equalTo(ureaTransporterPeptide)),
                contains( /* the rows are iterable */
                        closeTo(testData.data[5].zscore as Double, DELTA),
                        closeTo(testData.data[4].zscore as Double, DELTA)))))
    }

    def getDataMatcherForAnnotation(DeProteinAnnotation annotation,
                                    String property) {
        contains testData.data.
                findAll { it.annotation == annotation }.
                sort { it.assay.id }. // data is sorted by assay id
                collect { closeTo it."$property" as Double, DELTA }
    }

    void testLogIntensityProjection() {
        setupData()
        def logIntensityProjection = proteinResource.createProjection(
                [:], Projection.LOG_INTENSITY_PROJECTION)

        result = proteinResource.retrieveData(
                [trialConstraint], [], logIntensityProjection)

        def resultList = Lists.newArrayList result

        expect:
        that(resultList, containsInAnyOrder(
                testData.annotations.collect {
                    getDataMatcherForAnnotation it, 'logIntensity'
                }))
    }

    void testDefaultRealProjection() {
        setupData()
        def defaultRealProjection = proteinResource.createProjection(
                [:], Projection.DEFAULT_REAL_PROJECTION)

        result = proteinResource.retrieveData(
                [trialConstraint], [], defaultRealProjection)

        expect:
        that(result, hasItem(allOf(
                hasProperty('label', is(ureaTransporterPeptide)),
                contains(
                        closeTo(testData.data[5].intensity as Double, DELTA),
                        closeTo(testData.data[4].intensity as Double, DELTA)))))
    }

    void testSearchByProteinExternalIds() {
        setupData()
        def dataConstraint = proteinResource.createDataConstraint(
                DataConstraint.PROTEINS_CONSTRAINT,
                ids: testData.proteins.findAll {
                    it.name == 'Adiponectin' || it.name == 'Urea transporter 2'
                }*.externalId)

        result = proteinResource.retrieveData(
                [trialConstraint], [dataConstraint], projection)
        def resultList = Lists.newArrayList result

        expect:
        that(resultList, contains(
                hasProperty('label', is(ureaTransporterPeptide)),
                hasProperty('label', is(adiponectinPeptide))))
    }

    void testSearchByGenes() {
        setupData()
        def dataConstraint = proteinResource.createDataConstraint(
                DataConstraint.GENES_CONSTRAINT,
                names: ['AURKA'])
        // in our test data, gene AURKA is correlated with Adiponectin

        result = proteinResource.retrieveData(
                [trialConstraint], [dataConstraint], projection)
        def list = Lists.newArrayList(result)

        expect:
        that(list, contains(
                hasProperty('label', is(adiponectinPeptide))))
    }

    void testSearchByPathways() {
        setupData()
        def dataConstraint = proteinResource.createDataConstraint(
                DataConstraint.PATHWAYS_CONSTRAINT,
                names: ['FOOPATHWAY'])
        // in our test data, pathway FOOPATHWAY is correlated with Adiponectin

        result = proteinResource.retrieveData(
                [trialConstraint], [dataConstraint], projection)
        //TODO It returns empty result. It would be nice to investigate db state at this point
        expect:
        that(Lists.newArrayList(result), contains(
                hasProperty('label', is(adiponectinPeptide))))
    }

}
