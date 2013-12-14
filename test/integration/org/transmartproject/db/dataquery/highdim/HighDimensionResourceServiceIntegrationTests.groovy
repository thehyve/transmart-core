package org.transmartproject.db.dataquery.highdim

import com.google.common.collect.Iterables
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.Platform
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.UnsupportedByDataTypeException
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.querytool.QtQueryMaster

import static groovy.util.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.createTestPatients
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save
import static org.transmartproject.db.querytool.QueryResultData.createQueryResult
import static org.transmartproject.test.Matchers.hasSameInterfaceProperties

class HighDimensionResourceServiceIntegrationTests {

    HighDimensionResourceServiceTestData testData =
        new HighDimensionResourceServiceTestData()

    def highDimensionResourceService

    private AssayConstraint getAllPatientsPatientSetConstraint() {
        highDimensionResourceService.createAssayConstraint(
                result_instance_id: testData.allPatientsQueryResult.id,
                AssayConstraint.PATIENT_SET_CONSTRAINT)
    }

    @Before
    void setUp() {
        testData.saveAll()

        highDimensionResourceService.
                registerHighDimensionDataTypeModule('foobar') {
                    [
                            getDataTypeName: { -> 'foobar' },
                            matchesPlatform: { Platform p ->
                                p.markerType == 'Foobar' }
                    ] as HighDimensionDataTypeResource
                }
    }

    @Test
    void testGetSubResourcesAssayMultiMap() {
        Map<HighDimensionDataTypeResource, Long> res = highDimensionResourceService.
                getSubResourcesAssayMultiMap([ allPatientsPatientSetConstraint ])

        assertThat res.size(), is(2)
        assertThat res, allOf(
                hasEntry(
                        hasProperty('dataTypeName', is('mrna')),
                        containsInAnyOrder(
                                testData.mrnaAssays.collect {
                                    hasSameInterfaceProperties(Assay, it)
                                }
                        )),
                hasEntry(
                        hasProperty('dataTypeName', is('foobar')),
                        containsInAnyOrder(
                                testData.foobarAssays.collect {
                                    hasSameInterfaceProperties(Assay, it)
                                }
                        )))
    }

    @Test
    void testUnmappedPlatform() {
        def p = new DeGplInfo(
                markerType: 'bogus marker type',
        )
        p.id = 'bogus-platform'
        save([ p ])

        List<DeSubjectSampleMapping> assays =
                HighDimTestData.createTestAssays(testData.patientsFoobar, -7000, p,
                        HighDimensionResourceServiceTestData.TRIAL_NAME)
        save(assays)

        Map<HighDimensionDataTypeResource, Long> res = highDimensionResourceService.
                getSubResourcesAssayMultiMap([ allPatientsPatientSetConstraint ])

        assertThat res.values().inject([], { accum, cur -> accum + cur }), not(
                anyOf(
                        assays.collect { Assay it ->
                            hasItem(
                                    hasSameInterfaceProperties(Assay, it))
                        }
                ))
    }

    @Test
    void testAssaysWithMissingPlatform() {
        List<DeSubjectSampleMapping> assays =
                HighDimTestData.createTestAssays(testData.patientsFoobar, -7000, null,
                        HighDimensionResourceServiceTestData.TRIAL_NAME)
        save(assays)

        Map<HighDimensionDataTypeResource, Long> res = highDimensionResourceService.
                getSubResourcesAssayMultiMap([ allPatientsPatientSetConstraint ])

        assertThat res.values().inject([], { accum, cur -> accum + cur }), not(
                anyOf(
                        assays.collect { Assay it ->
                            hasItem(
                                    hasSameInterfaceProperties(Assay, it))
                        }
                ))
    }

    @Test
    void testWithMultipleConstraints() {
        def trialNameConstraint = highDimensionResourceService.createAssayConstraint(
                name: HighDimensionResourceServiceTestData.MRNA_TRIAL_NAME,
                AssayConstraint.TRIAL_NAME_CONSTRAINT)

        Map<HighDimensionDataTypeResource, Long> res = highDimensionResourceService.
                getSubResourcesAssayMultiMap([
                        allPatientsPatientSetConstraint,
                        trialNameConstraint ])

        assertThat res.size(), is(1)
        assertThat res, hasEntry(
                        hasProperty('dataTypeName', is('mrna')),
                        containsInAnyOrder(
                                testData.mrnaAssays.collect {
                                    hasSameInterfaceProperties(Assay, it)
                                }
                        ))
    }

    @Test
    void testBogusConstraint() {
        shouldFail InvalidArgumentsException, {
                highDimensionResourceService.createAssayConstraint([:],
                        'bogus constraint name')
        }
    }

    @Test
    void testInvalidParametersConstraint() {
        shouldFail InvalidArgumentsException, {
            highDimensionResourceService.createAssayConstraint(
                    foobar: [],
                    AssayConstraint.PATIENT_SET_CONSTRAINT)
        }
    }

    class HighDimensionResourceServiceTestData {

        static final String MRNA_TRIAL_NAME = 'MRNA_TRIAL_NAME'

        static final String TRIAL_NAME = 'HIGH_DIM_RESOURCE_TRIAL'

        DeGplInfo platformMrna = {
            def p = new DeGplInfo(
                    markerType: 'Gene Expression',
            )
            p.id = 'mrna-platform'
            p
        }()

        DeGplInfo platformFoobar = {
            def p = new DeGplInfo(
                    markerType: 'Foobar',
            )
            p.id = 'foobar-platform'
            p
        }()

        List<PatientDimension> patientsBoth = createTestPatients(2, -2000, TRIAL_NAME)
        List<PatientDimension> patientsFoobar = createTestPatients(3, -3000, TRIAL_NAME)

        List<DeSubjectSampleMapping> mrnaAssays =
            HighDimTestData.createTestAssays(patientsBoth, -4000, platformMrna, MRNA_TRIAL_NAME)

        List<DeSubjectSampleMapping> foobarAssays =
            HighDimTestData.createTestAssays(patientsBoth, -5000, platformFoobar, TRIAL_NAME) +
                    HighDimTestData.createTestAssays(patientsFoobar, -6000, platformFoobar, TRIAL_NAME)

        @Lazy QtQueryMaster allPatientsQueryMaster = createQueryResult(
                patientsBoth + patientsFoobar)

        QueryResult getAllPatientsQueryResult() {
            def f = { Iterables.getFirst it, null }
            f(f(allPatientsQueryMaster.queryInstances).queryResults)
        }

        void saveAll() {
            save([ platformMrna, platformFoobar ])
            save( patientsBoth + patientsFoobar )
            save( mrnaAssays + foobarAssays )
            save([ allPatientsQueryMaster ])
        }

    }



}
