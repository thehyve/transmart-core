package org.transmartproject.db.dataquery.highdim.mrna

import com.google.common.collect.Lists
import org.hibernate.ScrollableResults
import org.hibernate.engine.SessionImplementor
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.db.dataquery.highdim.*
import org.transmartproject.db.dataquery.highdim.assayconstraints.DefaultTrialNameConstraint
import org.transmartproject.db.dataquery.highdim.correlations.CorrelationTypesRegistry
import org.transmartproject.db.dataquery.highdim.correlations.SearchKeywordDataConstraint
import org.transmartproject.db.dataquery.highdim.dataconstraints.CriteriaDataConstraint
import org.transmartproject.db.dataquery.highdim.dataconstraints.DisjunctionDataConstraint
import org.transmartproject.db.dataquery.highdim.projections.SimpleRealProjection

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.createTestAssays
import static org.transmartproject.test.Matchers.hasSameInterfaceProperties

class MrnaDataRetrievalTests {

    private static final double DELTA = 0.0001

    @Autowired
    @Qualifier('mrnaModule')
    HighDimensionDataTypeModule mrnaModule

    HighDimensionDataTypeResource resource

    TabularResult dataQueryResult

    CorrelationTypesRegistry correlationTypesRegistry

    MrnaTestData testData = new MrnaTestData()

    DefaultTrialNameConstraint trialNameConstraint =
            new DefaultTrialNameConstraint(trialName: MrnaTestData.TRIAL_NAME)

    SimpleRealProjection rawIntensityProjection =
            new SimpleRealProjection(property: 'rawIntensity')

    @Before
    void setUp() {
        testData.saveAll()

        assertThat mrnaModule, is(notNullValue())

        resource = new HighDimensionDataTypeResourceImpl(mrnaModule)
    }

    @After
    void after() {
        if (dataQueryResult) {
            dataQueryResult.close()
        }
    }

    @Test
    void basicTest() {
        trialNameConstraint = new DefaultTrialNameConstraint(trialName: MrnaTestData.TRIAL_NAME)
        List assayConstraints = [trialNameConstraint]
        List dataConstraints = []

        dataQueryResult =
            resource.retrieveData assayConstraints, dataConstraints, rawIntensityProjection

        assertThat dataQueryResult, allOf(
                hasProperty('columnsDimensionLabel', equalTo('Sample codes')),
                hasProperty('rowsDimensionLabel',    equalTo('Probes')),
        )

        def resultList = Lists.newArrayList dataQueryResult

        assertThat resultList, allOf(
                hasSize(3),
                everyItem(isA(ProbeRow)),
                everyItem(
                        hasProperty('data',
                                allOf(
                                        hasSize(2),
                                        everyItem(isA(Double))
                                )
                        )
                ),
                everyItem(
                        hasProperty('assayIndexMap', allOf(
                                isA(Map),
                                hasEntry(
                                        /* assays are sorted in ascending order,
                                         * so -402 comes before -401 */
                                        hasProperty('id', equalTo(-402L)), /* key */
                                        equalTo(0),                        /* value */
                                ),
                                hasEntry(
                                        hasProperty('id', equalTo(-401L)),
                                        equalTo(1),
                                ),
                        ))
                ),
                contains(
                        allOf(
                            hasProperty('bioMarker', equalTo('BOGUSVNN3')),
                            hasProperty('label', equalTo('1553510_s_at')),
                        ),
                        hasProperty('bioMarker', equalTo('BOGUSRQCD1')),
                        hasProperty('bioMarker', equalTo('BOGUSCPO')),
                )
        )

        ProbeRow firstProbe = resultList.first()
        List<AssayColumn> lac = dataQueryResult.indicesList

        assertThat firstProbe.assayIndexMap.entrySet(), hasSize(2)

        // first probe is 1553510_s_at (gene BOGUSVNN3), as asserted before
        // intensities are 0.5 and 0.6
        assertThat firstProbe[0], allOf(
                closeTo(firstProbe[lac.find { it.id == -402L /*ascending order*/ }], DELTA),
                closeTo(0.6f, DELTA)
        )
        assertThat firstProbe[1], allOf(
                closeTo(firstProbe[lac.find { it.id == -401L }], DELTA),
                closeTo(0.5f, DELTA)
        )
    }

    private CriteriaDataConstraint createGenesDataConstraint(List skIds) {
        SearchKeywordDataConstraint.createForSearchKeywordIds(
                entityAlias:        'jProbe',
                propertyToRestrict: 'geneId',
                correlationTypes:
                        correlationTypesRegistry.getCorrelationTypesForTargetType('GENE'),
                skIds)
    }

    @Test
    void testWithGeneConstraint() {
        List assayConstraints = [trialNameConstraint]
        List dataConstraints = [
                createGenesDataConstraint([
                        testData.searchKeywords.
                                find({ it.keyword == 'BOGUSRQCD1' }).id
                ])
        ]
        dataQueryResult =
            resource.retrieveData assayConstraints, dataConstraints, rawIntensityProjection

        def resultList = Lists.newArrayList dataQueryResult

        assertThat resultList, allOf(
                hasSize(1),
                everyItem(hasProperty('data', hasSize(2))),
                contains(hasProperty('bioMarker', equalTo('BOGUSRQCD1')))
        )
    }

    @Test
    void testWithDisjunctionConstraint() {
        List assayConstraints = [trialNameConstraint]
        /* in this particular case, you could just use one constraint
         * and include two ids in the list */
        List dataConstraints = [
                new DisjunctionDataConstraint(constraints: [
                        createGenesDataConstraint([
                                testData.searchKeywords.find({ it.keyword == 'BOGUSRQCD1' }).id
                        ]),
                        createGenesDataConstraint([
                                testData.searchKeywords.find({ it.keyword == 'BOGUSVNN3' }).id
                        ])
                ])
        ]

        dataQueryResult =
            resource.retrieveData assayConstraints, dataConstraints, rawIntensityProjection

        def resultList = Lists.newArrayList dataQueryResult

        assertThat resultList, containsInAnyOrder(
                hasProperty('bioMarker', equalTo('BOGUSRQCD1')),
                hasProperty('bioMarker', equalTo('BOGUSVNN3')),
        )
    }

    private TabularResult testWithMissingDataAssay(Long baseAssayId) {
        def extraAssays = createTestAssays([ testData.patients[0] ], baseAssayId,
                testData.platform, MrnaTestData.TRIAL_NAME)
        HighDimTestData.save extraAssays

        List assayConstraints = [trialNameConstraint]

        dataQueryResult =
            resource.retrieveData assayConstraints, [], rawIntensityProjection
    }

    @Test
    void testWithMissingAssayLowestIdNumber() {
        testWithMissingDataAssay(-50000L)
        ((DefaultHighDimensionTabularResult) dataQueryResult).allowMissingAssays = true

        assertThat dataQueryResult.indicesList[0],
                hasSameInterfaceProperties(Assay, DeSubjectSampleMapping.get(-50001L))

        assertThat Lists.newArrayList(dataQueryResult.rows), everyItem(
                hasProperty('data', allOf(
                        hasSize(3), // for the three assays
                        contains(
                                is(nullValue()),
                                is(notNullValue()),
                                is(notNullValue()),
                        )
                ))
        )
    }

    @Test
    void testWithMissingAssayHighestIdNumber() {
        testWithMissingDataAssay(5000000L)
        ((DefaultHighDimensionTabularResult) dataQueryResult).allowMissingAssays = true

        assertThat dataQueryResult.indicesList[2],
                hasSameInterfaceProperties(Assay, DeSubjectSampleMapping.get(4999999L))

        assertThat Lists.newArrayList(dataQueryResult.rows), everyItem(
                hasProperty('data', allOf(
                        hasSize(3), // for the three assays
                        contains(
                                is(notNullValue()),
                                is(notNullValue()),
                                is(nullValue()),
                        )
                ))
        )
    }

    @Test
    @Ignore
    void testRepeatedDataPoint() {
        def assayConstraints = [trialNameConstraint]

        dataQueryResult =
                resource.retrieveData assayConstraints, [], rawIntensityProjection

        /* make the last element to be repeated */
        DefaultHighDimensionTabularResult castResult = dataQueryResult
        def origResults = castResult.results
        castResult.results = new ScrollableResults() {
            @Delegate
            ScrollableResults inner = origResults

            boolean stop = false
            Object last

            boolean next() {
                if (inner.next()) {
                    last = inner.get()
                    true
                } else if (!stop) {
                    stop = true
                    true
                } else {
                    false
                }
            }

            Object[] get() {
                last
            }

            SessionImplementor getSession() {
                inner.session
            }
        }

        assertThat shouldFail(UnexpectedResultException) {
            Lists.newArrayList(dataQueryResult)
        }, hasProperty('message', containsString('Got more assays than expected'))
    }

    @Test
    @Ignore
    void testWithMissingAssayDisallowMissingAssays() {
        testWithMissingDataAssay(-50000L)
        // default is not allowing missing assays

        assertThat shouldFail(UnexpectedResultException) {
            dataQueryResult.rows.next
        }, hasProperty('message', containsString('Assay ids not found: [-50001]'))
    }
}
