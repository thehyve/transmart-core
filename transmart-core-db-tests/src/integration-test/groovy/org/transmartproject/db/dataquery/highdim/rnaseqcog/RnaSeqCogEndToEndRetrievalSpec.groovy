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

package org.transmartproject.db.dataquery.highdim.rnaseqcog

import com.google.common.collect.Lists
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.dataquery.highdim.HighDimTestData
import org.transmartproject.db.dataquery.highdim.mirna.MirnaTestData
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.createTestAssays
import static org.transmartproject.db.test.Matchers.hasSameInterfaceProperties
import static spock.util.matcher.HamcrestSupport.that

@Integration
@Rollback
class RnaSeqCogEndToEndRetrievalSpec extends Specification {

    private static final double DELTA = 0.0001
    TabularResult<AssayColumn, RnaSeqCogDataRow> result

    RnaSeqCogTestData testData = new RnaSeqCogTestData()

    HighDimensionDataTypeResource<RnaSeqCogDataRow> rnaSeqCogResource

    HighDimensionResource highDimensionResourceService

    Projection projection

    AssayConstraint trialNameConstraint

    void setupData() {
        testData.saveAll()

        rnaSeqCogResource = highDimensionResourceService.
                getSubResourceForType('rnaseq_cog')

        projection = rnaSeqCogResource.createProjection(
                [:], Projection.ZSCORE_PROJECTION)

        trialNameConstraint = rnaSeqCogResource.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: RnaSeqCogTestData.TRIAL_NAME)
    }

    void cleanup() {
        result?.close()
    }

    void testBasic() {
        setupData()
        when:
        result = rnaSeqCogResource.retrieveData([trialNameConstraint],
                [], projection)

        then:
        that(result, allOf(
                hasProperty('columnsDimensionLabel', is('Sample codes')),
                hasProperty('rowsDimensionLabel', is('Transcripts')),
                hasProperty('indicesList', contains(
                        testData.assays.reverse().collect { Assay it ->
                            hasSameInterfaceProperties(Assay, it)
                        }.collect { is it }))))

        when:
        def rows = Lists.newArrayList result

        then:
        that(rows, contains(
                contains(testData.data[-5..-6]*.zscore.collect { Double it -> closeTo it, DELTA }),
                contains(testData.data[-3..-4]*.zscore.collect { Double it -> closeTo it, DELTA }),
                contains(testData.data[-1..-2]*.zscore.collect { Double it -> closeTo it, DELTA })))
    }

    void testDataRowsProperties() {
        setupData()
        result = rnaSeqCogResource.retrieveData([trialNameConstraint],
                [], projection)

        expect:
        that(Lists.newArrayList(result), contains(
                testData.annotations.collect { DeRnaseqAnnotation annotation ->
                    allOf(
                            hasProperty('label', is(annotation.id)),
                            hasProperty('bioMarker', is(annotation.geneSymbol)))
                }
        ))
    }

    void testLogIntensityProjection() {
        setupData()
        def logIntensityProjection = rnaSeqCogResource.createProjection(
                [:], Projection.LOG_INTENSITY_PROJECTION)

        result = rnaSeqCogResource.retrieveData(
                [trialNameConstraint], [], logIntensityProjection)

        def resultList = Lists.newArrayList(result)

        expect:
        that(resultList, containsInAnyOrder(
                testData.annotations.collect {
                    getDataMatcherForAnnotation(it, 'logIntensity')
                }))
    }

    void testDefaultRealProjection() {
        setupData()
        result = rnaSeqCogResource.retrieveData([trialNameConstraint], [],
                rnaSeqCogResource.createProjection([:], Projection.DEFAULT_REAL_PROJECTION))

        expect:
        that(Lists.newArrayList(result), hasItem(allOf(
                hasProperty('label', is(testData.data[-1].annotation.id)) /* VNN3 */,
                contains(testData.data[-1..-2]*.rawIntensity.collect { Double it -> closeTo it, DELTA })
        )))
    }

    void testGeneConstraint() {
        setupData()
        DataConstraint geneConstraint = rnaSeqCogResource.createDataConstraint(
                DataConstraint.GENES_CONSTRAINT,
                names: ['BOGUSVNN3'])

        result = rnaSeqCogResource.retrieveData([trialNameConstraint],
                [geneConstraint],
                rnaSeqCogResource.createProjection([:], Projection.DEFAULT_REAL_PROJECTION))

        expect:
        that(Lists.newArrayList(result), contains(
                hasProperty('bioMarker', is('BOGUSVNN3'))))
    }

    void testMissingAssaysAllowedSucceeds() {
        setupData()
        testWithMissingDataAssay(-50000L)
        expect:
        that(Lists.newArrayList(result.rows), everyItem(
                hasProperty('data', allOf(
                        hasSize(2), // for the three assays
                        contains(
                                is(notNullValue()),
                                is(notNullValue()),
                        )
                ))
        ))
    }

    private TabularResult testWithMissingDataAssay(Long baseAssayId) {
        def extraAssays = createTestAssays([testData.patients[0]], baseAssayId,
                testData.platform, MirnaTestData.TRIAL_NAME)
        HighDimTestData.save extraAssays

        List assayConstraints = [trialNameConstraint]

        result =
                rnaSeqCogResource.retrieveData assayConstraints, [], projection
    }

    def getDataMatcherForAnnotation(DeRnaseqAnnotation annotation,
                                    String property) {
        contains testData.data.
                findAll { it.annotation == annotation }.
                sort { it.assay.id }. // data is sorted by assay id
                collect { closeTo it."$property" as Double, DELTA }
    }

}
