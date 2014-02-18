package org.transmartproject.db.dataquery.highdim.metabolite

import com.google.common.collect.Lists
import org.hamcrest.Matcher
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

class MetaboliteEndToEndRetrievalTest {

    /* scale of the zscore column is 5, so this is the max rounding error */
    private static Double DELTA = 0.000005

    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource metaboliteResource

    @Lazy AssayConstraint trialConstraint = metaboliteResource.createAssayConstraint(
            AssayConstraint.TRIAL_NAME_CONSTRAINT,
            name: MetaboliteTestData.TRIAL_NAME)

    @Lazy Projection projection = metaboliteResource.createProjection([:],
            org.transmartproject.core.dataquery.highdim.projections.Projection.ZSCORE_PROJECTION)

    TabularResult<AssayColumn, MetaboliteDataRow> result

    MetaboliteTestData testData = new MetaboliteTestData()

    @Before
    void setup() {
        testData.saveAll()
        metaboliteResource = highDimensionResourceService.getSubResourceForType('metabolite')
    }

    @After
    void tearDown() {
        result?.close()
    }

    @Test
    void fetchAllDataTest() {
        result = metaboliteResource.retrieveData([trialConstraint], [], projection)

        List rows = Lists.newArrayList result.rows

        assertThat rows, contains(
                testData.annotations.sort { it.id }.
                collect { annotation ->
                    allOf(
                            isA(MetaboliteDataRow),
                            hasProperty('label', is(annotation.biochemicalName)),
                            hasProperty('bioMarker', is(annotation.hmdbId)),
                            contains(matchersForDataPoints([annotation])))
                }
        )
    }

    @Test
    void testLogIntensityProjection() {
        def logIntensityProjection = metaboliteResource.createProjection(
                [:], Projection.LOG_INTENSITY_PROJECTION)

        result = metaboliteResource.retrieveData(
                [ trialConstraint ], [], logIntensityProjection)

        def resultList = Lists.newArrayList(result)

        assertThat(
                resultList.collect { it.data }.flatten(),
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

    @Test
    void searchWithHmdbId() {
        def hmdbid = 'HMDB30537'
        def dataConstraint = metaboliteResource.createDataConstraint(
                'metabolites',
                names: [hmdbid])

        result = metaboliteResource.retrieveData(
                [ trialConstraint ], [ dataConstraint ], projection)

        assertThat Lists.newArrayList(result), contains(allOf(
                isA(MetaboliteDataRow),
                hasProperty('bioMarker', is(hmdbid)),
                contains(matchersForDataPoints(
                        testData.annotations.findAll { it.hmdbId == hmdbid }))))
    }

    @Test
    void testSearchWithSubPathway() {
        def subpathway = 'Pentose Metabolism' // testData.subPathways[3].name
        def dataConstraint = metaboliteResource.createDataConstraint(
                'metabolite_subpathways',
                names: [ subpathway ])

        result = metaboliteResource.retrieveData(
                [ trialConstraint ], [ dataConstraint ], projection)

        Collection<DeMetaboliteAnnotation> annotations =
                testData.subPathways.find { it.name == subpathway }.annotations

        //should find only HMDB30536, just a sanity check here
        assertThat annotations, contains(hasProperty('id', is(-503L)))
        DeMetaboliteAnnotation annotation = annotations.iterator().next()

        def res = Lists.newArrayList(result)

        assertThat res, contains(allOf(
                isA(MetaboliteDataRow),
                hasProperty('bioMarker', is(annotation.hmdbId)),
                contains(matchersForDataPoints([annotation]))))
    }

    @Test
    void testSearchWithSubPathwayViaSearchKeywordId() {
        def subPathwayName = 'No superpathway subpathway'
        def searchKeyword = testData.searchKeywordsForSubPathways.find {
            it.keyword == subPathwayName
        }

        def dataConstraint = metaboliteResource.createDataConstraint(
                DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT,
                keyword_ids: [ searchKeyword.id ])

        result = metaboliteResource.retrieveData(
                [ trialConstraint ], [ dataConstraint ], projection)

        Collection<DeMetaboliteAnnotation> annotations =
                testData.subPathways.find { it.name == subPathwayName }.annotations

        //should find only HMDB30536, just a sanity check here
        assertThat annotations, contains(hasProperty('id', is(-502L)))
        DeMetaboliteAnnotation annotation = annotations.iterator().next()

        def res = Lists.newArrayList(result)

        assertThat res, contains(allOf(
                isA(MetaboliteDataRow),
                hasProperty('bioMarker', is(annotation.hmdbId)),
                contains(matchersForDataPoints([annotation]))))
    }

    @Test
    void testSearchWithSuperPathway() {
        def superPathway = 'Phosphoric Acid'
        /* goes to subpathways 3 */
        def dataConstraint = metaboliteResource.createDataConstraint(
                'metabolite_superpathways',
                names: [ superPathway ])

        result = metaboliteResource.retrieveData(
                [ trialConstraint ], [ dataConstraint ], projection)

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

        assertThat res, contains(allOf(
                isA(MetaboliteDataRow),
                hasProperty('bioMarker', is(annotation.hmdbId)),
                contains(matchersForDataPoints([annotation]))))
    }

    @Test
    void testFindsAssays() {
        def assayConstraint = highDimensionResourceService.
                createAssayConstraint(AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: MetaboliteTestData.TRIAL_NAME)

        def res = highDimensionResourceService.
                getSubResourcesAssayMultiMap([assayConstraint])

        assertThat res, hasEntry(
                hasProperty('dataTypeName', is('metabolite')),
                containsInAnyOrder(
                        testData.assays.collect {
                            hasSameInterfaceProperties(Assay, it)
                        }
                )
        )
    }

}
