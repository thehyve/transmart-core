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

package org.transmartproject.db.dataquery.highdim.metabolite

import com.google.common.collect.Lists
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.hamcrest.Matcher
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import spock.lang.Specification

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.db.test.Matchers.hasSameInterfaceProperties
import static spock.util.matcher.HamcrestSupport.that

@Integration
@Rollback
class MetaboliteEndToEndRetrievalSpec extends Specification {

    /* scale of the zscore column is 5, so this is the max rounding error */
    private static Double DELTA = 0.000005

    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource metaboliteResource

    AssayConstraint trialConstraint

    Projection projection

    TabularResult<AssayColumn, MetaboliteDataRow> result

    MetaboliteTestData testData = new MetaboliteTestData()

    void setupData() {
        testData.saveAll()
        metaboliteResource = highDimensionResourceService.getSubResourceForType('metabolite')

        trialConstraint = metaboliteResource.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: MetaboliteTestData.TRIAL_NAME)
        projection = metaboliteResource.createProjection([:],
                org.transmartproject.core.dataquery.highdim.projections.Projection.ZSCORE_PROJECTION)
    }

    void cleanup() {
        result?.close()
    }

    void testFetchAllData() {
        setupData()
        result = metaboliteResource.retrieveData([trialConstraint], [], projection)

        List rows = Lists.newArrayList result.rows

        expect:
        that(rows, contains(
                testData.annotations.sort { it.id }.
                        collect { annotation ->
                            allOf(
                                    isA(MetaboliteDataRow),
                                    hasProperty('label', is(annotation.biochemicalName)),
                                    hasProperty('bioMarker', is(annotation.hmdbId)),
                                    contains(matchersForDataPoints([annotation])))
                        }
        ))
    }

    void testLogIntensityProjection() {
        setupData()
        def logIntensityProjection = metaboliteResource.createProjection(
                [:], Projection.LOG_INTENSITY_PROJECTION)

        result = metaboliteResource.retrieveData(
                [trialConstraint], [], logIntensityProjection)

        def resultList = Lists.newArrayList(result)

        expect:
        that(resultList.collect { it.data }.flatten(),
                containsInAnyOrder(testData.data.collect { closeTo(it.logIntensity as Double, DELTA) })
        )
    }

    private List<Matcher> matchersForDataPoints(List<DeMetaboliteAnnotation> annotations) {
        testData.data.findAll { it.annotation.id in annotations*.id }.
                sort { a, b ->
                    a.annotation.id <=> b.annotation.id ?:
                            a.assay.id <=> b.assay.id
                }.
                collect { closeTo(it.zscore as Double, DELTA) }
    }

    void testSearchWithHmdbId() {
        setupData()
        def hmdbid = 'HMDB30537'
        def dataConstraint = metaboliteResource.createDataConstraint(
                'metabolites',
                names: [hmdbid])

        result = metaboliteResource.retrieveData(
                [trialConstraint], [dataConstraint], projection)
        def list = Lists.newArrayList(result)

        expect:
        that(list, contains(allOf(
                isA(MetaboliteDataRow),
                hasProperty('bioMarker', is(hmdbid)),
                contains(matchersForDataPoints(
                        testData.annotations.findAll { it.hmdbId == hmdbid })))))
    }

    void testSearchWithSubPathway() {
        setupData()
        def subpathway = 'Pentose Metabolism' // testData.subPathways[3].name
        def dataConstraint = metaboliteResource.createDataConstraint(
                'metabolite_subpathways',
                names: [subpathway])

        result = metaboliteResource.retrieveData(
                [trialConstraint], [dataConstraint], projection)

        Collection<DeMetaboliteAnnotation> annotations =
                testData.subPathways.find { it.name == subpathway }.annotations

        //should find only HMDB30536, just a sanity check here
        assertThat annotations, contains(hasProperty('id', is(-503L)))
        DeMetaboliteAnnotation annotation = annotations.iterator().next()

        def res = Lists.newArrayList(result)

        expect:
        that(res, contains(allOf(
                isA(MetaboliteDataRow),
                hasProperty('bioMarker', is(annotation.hmdbId)),
                contains(matchersForDataPoints([annotation])))))
    }

    void testSearchWithSubPathwayViaSearchKeywordId() {
        setupData()
        def subPathwayName = 'No superpathway subpathway'
        def searchKeyword = testData.searchKeywordsForSubPathways.find {
            it.keyword == subPathwayName
        }

        def dataConstraint = metaboliteResource.createDataConstraint(
                DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT,
                keyword_ids: [searchKeyword.id])

        result = metaboliteResource.retrieveData(
                [trialConstraint], [dataConstraint], projection)

        Collection<DeMetaboliteAnnotation> annotations =
                testData.subPathways.find { it.name == subPathwayName }.annotations

        //should find only HMDB30536, just a sanity check here
        assertThat annotations, contains(hasProperty('id', is(-502L)))
        DeMetaboliteAnnotation annotation = annotations.iterator().next()

        def res = Lists.newArrayList(result)

        expect:
        that(res, contains(allOf(
                isA(MetaboliteDataRow),
                hasProperty('bioMarker', is(annotation.hmdbId)),
                contains(matchersForDataPoints([annotation])))))
    }

    void testSearchWithSuperPathway() {
        setupData()
        def superPathway = 'Phosphoric Acid'
        /* goes to subpathways 3 */
        def dataConstraint = metaboliteResource.createDataConstraint(
                'metabolite_superpathways',
                names: [superPathway])

        result = metaboliteResource.retrieveData(
                [trialConstraint], [dataConstraint], projection)

        Collection<DeMetaboliteSubPathway> subPathways =
                testData.superPathways.find {
                    it.name == superPathway
                }.refresh().subPathways

        /* goes to ann -503 via subpathway -704 */
        assertThat subPathways, contains(hasProperty('id', is(-704L)))
        Collection<DeMetaboliteAnnotation> annotations =
                subPathways.iterator().next().annotations

        //should find only HMDB30536, -503
        assertThat annotations, contains(hasProperty('id', is(-503L)))
        DeMetaboliteAnnotation annotation = annotations.iterator().next()

        def res = Lists.newArrayList(result)

        expect:
        that(res, contains(allOf(
                isA(MetaboliteDataRow),
                hasProperty('bioMarker', is(annotation.hmdbId)),
                contains(matchersForDataPoints([annotation])))))
    }

    void testFindsAssays() {
        setupData()
        def assayConstraint = highDimensionResourceService.
                createAssayConstraint(AssayConstraint.TRIAL_NAME_CONSTRAINT,
                        name: MetaboliteTestData.TRIAL_NAME)

        def res = highDimensionResourceService.
                getSubResourcesAssayMultiMap([assayConstraint])

        expect:
        that(res, hasEntry(
                hasProperty('dataTypeName', is('metabolite')),
                containsInAnyOrder(
                        testData.assays.collect {
                            hasSameInterfaceProperties(Assay, it)
                        }
                )
        ))
    }

    void testNaNInZscore() {
        setupData()
        // with the current impl of the loading job in transmart-batch,
        // zscore can be NaN if the std dev is 0
        def dataPoint = testData.data[0]
        dataPoint.zscore = Double.NaN
        dataPoint.save(flush: true)

        def assayConstraint = highDimensionResourceService.
                createAssayConstraint(AssayConstraint.ASSAY_ID_LIST_CONSTRAINT,
                        ids: [dataPoint.assay.id])

        result = metaboliteResource.retrieveData(
                [assayConstraint], [], projection)

        def res = Lists.newArrayList(result)

        expect:
        that(res, hasItem(contains(Double.NaN)))
    }

}
