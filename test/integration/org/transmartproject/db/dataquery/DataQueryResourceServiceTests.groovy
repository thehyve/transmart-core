package org.transmartproject.db.dataquery

import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.Platform
import org.transmartproject.core.dataquery.acgh.ACGHValues
import org.transmartproject.core.dataquery.acgh.Region
import org.transmartproject.core.dataquery.acgh.RegionRow
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.constraints.ACGHRegionQuery
import org.transmartproject.core.dataquery.constraints.CommonHighDimensionalQueryConstraints
import org.transmartproject.db.highdim.DeGplInfo
import org.transmartproject.db.highdim.DeSubjectSampleMapping
import org.transmartproject.db.highdim.HighDimTestData
import org.transmartproject.db.querytool.QtQueryResultInstance
import org.transmartproject.db.querytool.QueryResultData

import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.Assert.fail
import static org.hamcrest.Matchers.*

@Mixin(HighDimTestData)
@Mixin(QueryResultData)
class DataQueryResourceServiceTests {

    def sessionFactory
    def dataQueryResourceService
    def resultInstance

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
        def result = dataQueryResourceService.runACGHRegionQuery(q, null)

        assertThat result, allOf(
                is(notNullValue()),
                hasProperty('indicesList', contains(
                        /* they're ordered by assay id */
                        equalTo(testRegionAssays[1]),
                        equalTo(testRegionAssays[0]),
                ))
        )

        Iterator<RegionRow> rows = result.rows
        def regionRows = Lists.newArrayList(rows)

        assertThat regionRows, hasSize(2)
        /* results are ordered (asc) by region id */
        assertThat regionRows[0],
                hasProperty('region', equalTo(testRegions[1]))
        assertThat regionRows[1],
                hasProperty('region', equalTo(testRegions[0]))

        assertThat regionRows[0].getRegionDataForAssay(testRegionAssays[0]),
                equalTo(testACGHData[2])
        assertThat regionRows[0].getRegionDataForAssay(testRegionAssays[1]),
                equalTo(testACGHData[3])
        assertThat regionRows[1].getRegionDataForAssay(testRegionAssays[0]),
                equalTo(testACGHData[0])
        assertThat regionRows[1].getRegionDataForAssay(testRegionAssays[1]),
                equalTo(testACGHData[1])
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
                *:base,
                trialName: 'TRIAL_NOT_MATCHING'
        ])
        a.id = -4002
        assays << a

        a = new DeSubjectSampleMapping([
                *:base,
                timepointCd: 'non_match'
        ])
        a.id = -4003
        assays << a

        a = new DeSubjectSampleMapping([
                *:base,
                sampleTypeCd: 'non_match'
        ])
        a.id = -4004
        assays << a

        a = new DeSubjectSampleMapping([
                *:base,
                tissueTypeCd: 'non_match'
        ])
        a.id = -4005
        assays << a

        a = new DeSubjectSampleMapping([
                *:base,
                platform: extraPlatform
        ])
        a.id = -4006
        assays << a

        assertThat extraPlatform.save(), isA(Platform)
        assertThat assays*.save(), everyItem(isA(Assay))

        assertThat assays.collect { assay ->
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

        def result = dataQueryResourceService.runACGHRegionQuery(q, null)

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
        def result = dataQueryResourceService.runACGHRegionQuery(q, session)

        assertThat result, hasProperty('indicesList', hasSize(2))
        def regionRows = Lists.newArrayList(result.rows)
        assertThat regionRows, hasSize(2)
    }

    @Test
    void testRequiredTrialsConstraints() {
        def q = new ACGHRegionQuery(
                common: new CommonHighDimensionalQueryConstraints(
                        patientQueryResult: resultInstance
                ),
        )

        try {
            dataQueryResourceService.runACGHRegionQuery(q, null)
            fail("Expected exception")
        } catch (e) {
            assertThat(e, allOf(
                    instanceOf(IllegalArgumentException),
                    hasProperty('message', equalTo('query.common.studies not ' +
                            'specified/empty'))
            ))
        }
    }


    @Test
    void testRequiredPatientSet() {
        def q = new ACGHRegionQuery(
                common: new CommonHighDimensionalQueryConstraints(
                        studies: [testRegionAssays[0].trialName],
                ),
        )

        try {
            dataQueryResourceService.runACGHRegionQuery(q, null)
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
