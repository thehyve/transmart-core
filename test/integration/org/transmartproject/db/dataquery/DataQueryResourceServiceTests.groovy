package org.transmartproject.db.dataquery

import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.Platform
import org.transmartproject.core.dataquery.acgh.ACGHValues
import org.transmartproject.core.dataquery.acgh.ChromosomalSegment
import org.transmartproject.core.dataquery.acgh.Region
import org.transmartproject.core.dataquery.acgh.RegionRow
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.constraints.ACGHRegionQuery
import org.transmartproject.core.dataquery.constraints.CommonHighDimensionalQueryConstraints
import org.transmartproject.db.highdim.DeChromosomalRegion
import org.transmartproject.db.highdim.DeGplInfo
import org.transmartproject.db.highdim.DeSubjectSampleMapping
import org.transmartproject.db.querytool.QtQueryResultInstance

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.junit.Assert.assertEquals
import static org.junit.Assert.fail
import static org.transmartproject.test.Matchers.hasSameInterfaceProperties

abstract class DataQueryResourceServiceTests {

    def sessionFactory
    def resultInstance
    def testedService

    @Before
    void setUp() {
        def queryMaster
        assertThat testRegionPlatform.save(), isA(Platform)
        assertThat testRegions*.save(), everyItem(isA(Region))
        assertThat testRegionPatients*.save(), everyItem(isA(Patient))
        assertThat testRegionAssays*.save(), everyItem(isA(Assay))
        assertThat testACGHData*.save(), everyItem(isA(ACGHValues))
        queryMaster = createQueryResult(testRegionPatients).save()
        assertThat queryMaster, is(notNullValue())

        def f = Iterables.&getLast
        resultInstance = f(f(queryMaster.queryInstances).queryResults)
        assertThat resultInstance, allOf(
                is(notNullValue()),
                hasProperty('attached', is(true))
        )
    }

    @Test
    void basicTest() {
        def q = new ACGHRegionQuery(
                common: new CommonHighDimensionalQueryConstraints(
                        studies: [testRegionAssays[0].trialName],
                        patientQueryResult: resultInstance
                ),
        )
        def result = testedService.runACGHRegionQuery(q, null)

        assertThat result, allOf(
                is(notNullValue()),
                hasProperty('indicesList', contains(
                        /* they're ordered by assay id */
                        hasSameInterfaceProperties(Assay, testRegionAssays[1], ['platform']),
                        hasSameInterfaceProperties(Assay, testRegionAssays[0], ['platform']),
                ))
        )

        Iterator<RegionRow> rows = result.rows
        def regionRows = Lists.newArrayList(rows)

        assertThat regionRows, hasSize(2)
        /* results are ordered (asc) by region id */
        assertThat regionRows[0],
                hasProperty('region', hasSameInterfaceProperties(Region, testRegions[1], ['platform']))
        assertThat regionRows[1],
                hasProperty('region', hasSameInterfaceProperties(Region, testRegions[0], ['platform']))

        assertThat regionRows[0].getRegionDataForAssay(testRegionAssays[0]),
                hasSameInterfaceProperties(ACGHValues, testACGHData[2])
        assertThat regionRows[0].getRegionDataForAssay(testRegionAssays[1]),
                hasSameInterfaceProperties(ACGHValues, testACGHData[3])
        assertThat regionRows[1].getRegionDataForAssay(testRegionAssays[0]),
                hasSameInterfaceProperties(ACGHValues, testACGHData[0])
        assertThat regionRows[1].getRegionDataForAssay(testRegionAssays[1]),
                hasSameInterfaceProperties(ACGHValues, testACGHData[1])
    }

    @Test
    void testGetChromosomalSegments() {
        def q = new ACGHRegionQuery(
                common: new CommonHighDimensionalQueryConstraints(
                        patientQueryResult: resultInstance
                ),
        )
        def result = testedService.getChromosomalSegments(q)
        assertThat result, allOf(hasSize(2),
                containsInAnyOrder(
                        new ChromosomalSegment(chromosome: '2', start: 66L, end: 99L),
                        new ChromosomalSegment(chromosome: '1', start: 33L, end: 9999L)
                )
        )
    }

    @Test
    void testSegments_meetOne() {
        def q = new ACGHRegionQuery(
                common: new CommonHighDimensionalQueryConstraints(
                        patientQueryResult: resultInstance
                ),
                //In this case just start of tested range belong to segment and it get into results
                segments: [ new ChromosomalSegment(chromosome: '1', start: 33, end: 44) ]
        )
        def result = testedService.runACGHRegionQuery(q, null)
        Iterator<RegionRow> rows = result.rows
        def regionRows = Lists.newArrayList(rows)

        assertThat regionRows, hasSize(1)
        assertEquals '1', regionRows[0].getRegion().getChromosome()
    }

    @Test
    void testSegments_meetBoth() {
        def q = new ACGHRegionQuery(
                common: new CommonHighDimensionalQueryConstraints(
                        patientQueryResult: resultInstance
                ),
                segments: [
                        //In this case tested region wider then the segment and it get into results
                        new ChromosomalSegment(chromosome: '1', start: 44, end: 8888),
                        //In this case just end of tested region belong to segment and it get into results
                        new ChromosomalSegment(chromosome: '2', start: 88, end: 99) ]
        )

        def anotherPlatform = new DeGplInfo(
                title: 'Another Test Region Platform',
                organism: 'Homo Sapiens',
                annotationDate: Date.parse('yyyy-MM-dd', '2013-08-03'),
                markerTypeId: DeChromosomalRegion.MARKER_TYPE.id,
                genomeBuild: 'genome build #2',
        )
        anotherPlatform.id = 'test-another-platform'
        anotherPlatform.save(failOnError: true)
        //NOTE: This region should not apear in result set
        def anotherRegion = new DeChromosomalRegion(
                platform: anotherPlatform,
                chromosome: '1',
                start: 1,
                end: 10,
                numberOfProbes: 42,
                name: 'region 1:1-10'
        )
        anotherRegion.id = -2000L
        anotherRegion.save(failOnError: true)

        def result = testedService.runACGHRegionQuery(q, null)
        Iterator<RegionRow> rows = result.rows
        def regionRows = Lists.newArrayList(rows)

        assertThat regionRows, hasSize(2)
        assertEquals '2', regionRows[0].getRegion().getChromosome()
        assertEquals '1', regionRows[1].getRegion().getChromosome()
    }

    @Test
    void testSegmentss_meetNone() {
        def q = new ACGHRegionQuery(
                common: new CommonHighDimensionalQueryConstraints(
                        patientQueryResult: resultInstance
                ),
                segments: [
                        new ChromosomalSegment(chromosome: 'X'),
                        new ChromosomalSegment(chromosome: '1', start: 1, end: 32),
                        new ChromosomalSegment(chromosome: '2', start: 100, end: 1000)
                ]
        )
        def result = testedService.runACGHRegionQuery(q, null)
        def regionRows = Lists.newArrayList(result.rows)

        assertThat regionRows, hasSize(0)
    }

    @Test
    void testRemainingCommonParameters() {
        def base = [
                patient: testRegionPatients[0],
                subjectId: testRegionPatients[0].inTrialId,
                trialName: 'REGION_SAMP_TRIAL',
                timepointCd: 'match timepointcd',
                sampleTypeCd: 'match sampletypecd',
                tissueTypeCd: 'match tissuetypecd',
                platform: testRegionPlatform]
        def extraPlatform = new DeGplInfo()
        List<DeSubjectSampleMapping> assays = []
        def a

        extraPlatform.id = 'no match platform'

        a = new DeSubjectSampleMapping(base)
        a.id = -4001
        assays << a

        a = new DeSubjectSampleMapping([
                *: base,
                trialName: 'TRIAL_NOT_MATCHING'
        ])
        a.id = -4002
        assays << a

        a = new DeSubjectSampleMapping([
                *: base,
                timepointCd: 'non_match'
        ])
        a.id = -4003
        assays << a

        a = new DeSubjectSampleMapping([
                *: base,
                sampleTypeCd: 'non_match'
        ])
        a.id = -4004
        assays << a

        a = new DeSubjectSampleMapping([
                *: base,
                tissueTypeCd: 'non_match'
        ])
        a.id = -4005
        assays << a

        a = new DeSubjectSampleMapping([
                *: base,
                platform: extraPlatform
        ])
        a.id = -4006
        assays << a

        assertThat extraPlatform.save(), isA(Platform)
        assertThat assays*.save(), everyItem(isA(Assay))

        assertThat assays.collect {assay ->
            createACGHData(testRegions[0], assay, -1)
        }*.save(), everyItem(isA(ACGHValues))

        def q = new ACGHRegionQuery(
                common: new CommonHighDimensionalQueryConstraints(
                        studies: [base.trialName],
                        patientQueryResult: resultInstance,
                        platforms: [base.platform],
                        sampleCodes: [base.sampleTypeCd],
                        timepointCodes: [base.timepointCd],
                        tissueCodes: [base.tissueTypeCd]
                ),
        )

        def result = testedService.runACGHRegionQuery(q, null)

        assertThat result, hasProperty('indicesList', contains(
                hasProperty('id', equalTo(-4001L))
        ))
    }

    @Test
    void testStatelessSession() {
        sessionFactory.currentSession.flush()

        def session = sessionFactory.openStatelessSession()
        def q = new ACGHRegionQuery(
                common: new CommonHighDimensionalQueryConstraints(
                        studies: [testRegionAssays[0].trialName],
                        patientQueryResult: session.get(
                                QtQueryResultInstance, resultInstance.id)
                ),
        )
        def result = testedService.runACGHRegionQuery(q, session)

        assertThat result, hasProperty('indicesList', hasSize(2))
        def regionRows = Lists.newArrayList(result.rows)
        assertThat regionRows, hasSize(2)
    }

    @Test
    void testRequiredPatientSet() {
        def q = new ACGHRegionQuery(
                common: new CommonHighDimensionalQueryConstraints(
                        studies: [testRegionAssays[0].trialName],
                ),
        )

        try {
            testedService.runACGHRegionQuery(q, null)
            fail("Expected exception")
        } catch (e) {
            assertThat(e, allOf(
                    instanceOf(IllegalArgumentException),
                    hasProperty('message', equalTo(
                            'query.common.patientQueryResult not ' +
                                    'specified/empty'))
            ))
        }
    }
}
