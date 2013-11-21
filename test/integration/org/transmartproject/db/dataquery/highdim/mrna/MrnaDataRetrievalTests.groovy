package org.transmartproject.db.dataquery.highdim.mrna

import com.google.common.collect.Iterators
import com.google.common.collect.Lists
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.dataquery.highdim.DefaultHighDimensionTabularResult
import org.transmartproject.db.dataquery.highdim.HighDimTestData
import org.transmartproject.db.dataquery.highdim.HighDimensionDataTypeModule
import org.transmartproject.db.dataquery.highdim.HighDimensionDataTypeResourceImpl
import org.transmartproject.db.dataquery.highdim.assayconstraints.DefaultTrialNameConstraint
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

    @Before
    void setUp() {
        MrnaTestData.saveAll()

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
        List assayConstraints = [
                new DefaultTrialNameConstraint(trialName: MrnaTestData.TRIAL_NAME)
        ]
        List dataConstraints = []
        def projection = new SimpleRealProjection(property: 'rawIntensity')

        dataQueryResult =
            resource.retrieveData assayConstraints, dataConstraints, projection

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
                            hasProperty('geneSymbol', equalTo('BOGUSVNN3')),
                            hasProperty('label', equalTo('1553510_s_at')),
                        ),
                        hasProperty('geneSymbol', equalTo('BOGUSRQCD1')),
                        hasProperty('geneSymbol', equalTo('BOGUSCPO')),
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

    @Test
    void testWithGeneConstraint() {
        List assayConstraints = [
                new DefaultTrialNameConstraint(trialName: MrnaTestData.TRIAL_NAME)
        ]
        List dataConstraints = [
                MrnaGeneDataConstraint.createForLongIds([
                        MrnaTestData.searchKeywords.
                                find({ it.keyword == 'BOGUSRQCD1' }).uniqueId
                ])
        ]
        def projection = new SimpleRealProjection(property: 'rawIntensity')

        dataQueryResult =
            resource.retrieveData assayConstraints, dataConstraints, projection

        def resultList = Lists.newArrayList dataQueryResult

        assertThat resultList, allOf(
                hasSize(1),
                everyItem(hasProperty('data', hasSize(2))),
                contains(hasProperty('geneSymbol', equalTo('BOGUSRQCD1')))
        )
    }

    @Test
    void testWithDisjunctionConstraint() {
        List assayConstraints = [
                new DefaultTrialNameConstraint(trialName: MrnaTestData.TRIAL_NAME)
        ]
        /* in this particular case, you could just use one MrnaGeneDataConstraint
         * and include two ids in the list */
        List dataConstraints = [
                new DisjunctionDataConstraint(constraints: [
                        MrnaGeneDataConstraint.createForLongIds([
                                MrnaTestData.searchKeywords.find({ it.keyword == 'BOGUSRQCD1' }).uniqueId
                        ]),
                        MrnaGeneDataConstraint.createForLongIds([
                                MrnaTestData.searchKeywords.find({ it.keyword == 'BOGUSVNN3' }).uniqueId
                        ])
                ])
        ]

        def projection = new SimpleRealProjection(property: 'rawIntensity')

        dataQueryResult =
            resource.retrieveData assayConstraints, dataConstraints, projection

        def resultList = Lists.newArrayList dataQueryResult

        assertThat resultList, containsInAnyOrder(
                hasProperty('geneSymbol', equalTo('BOGUSRQCD1')),
                hasProperty('geneSymbol', equalTo('BOGUSVNN3')),
        )
    }

    private TabularResult testWithMissingDataAssay(Long baseAssayId) {
        def extraAssays = createTestAssays([ MrnaTestData.patients[0] ], baseAssayId,
                MrnaTestData.platform, MrnaTestData.TRIAL_NAME)
        HighDimTestData.save extraAssays

        List assayConstraints = [
                new DefaultTrialNameConstraint(trialName: MrnaTestData.TRIAL_NAME)
        ]

        def projection = new SimpleRealProjection(property: 'rawIntensity')

        dataQueryResult =
            resource.retrieveData assayConstraints, [], projection
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
    void testWithMissingAssayDisallowMissingAssays() {
        testWithMissingDataAssay(-50000L)
        // default is not allowing missing assays

        assertThat shouldFail(UnexpectedResultException) {
            dataQueryResult.rows.next
        }, hasProperty('message', containsString('Assay ids not found: [-50001]'))
    }
}
