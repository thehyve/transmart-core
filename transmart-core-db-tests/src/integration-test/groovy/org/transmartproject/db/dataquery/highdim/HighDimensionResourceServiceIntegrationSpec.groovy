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

package org.transmartproject.db.dataquery.highdim

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.Platform
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.TestData
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.querytool.QtQueryMaster
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.createTestPatients
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save
import static org.transmartproject.db.querytool.QueryResultData.createQueryResult
import static org.transmartproject.db.querytool.QueryResultData.getQueryResultFromMaster
import static org.transmartproject.db.test.Matchers.hasSameInterfaceProperties
import static spock.util.matcher.HamcrestSupport.that

@Integration
@Rollback
class HighDimensionResourceServiceIntegrationSpec extends Specification {

    private static final String TEST_DATA_TYPE = 'foobar'

    @Autowired
    HighDimensionResourceService highDimensionResourceService

    HighDimensionResourceServiceTestData testData =
            new HighDimensionResourceServiceTestData()

    private AssayConstraint getAllPatientsPatientSetConstraint() {
        highDimensionResourceService.createAssayConstraint(
                result_instance_id: testData.allPatientsQueryResult.id,
                AssayConstraint.PATIENT_SET_CONSTRAINT)
    }

    void setupData() {
        TestData.prepareCleanDatabase()

        testData.saveAll()

        def bogusDataTypeResource = [
                getDataTypeName: { -> TEST_DATA_TYPE },
                matchesPlatform: { Platform p ->
                    p.markerType == 'Foobar'
                },
        ] as HighDimensionDataTypeResource

        highDimensionResourceService.
                registerHighDimensionDataTypeModule(TEST_DATA_TYPE) {
                    // returns always the same instance
                    // this is not strictly require
                    bogusDataTypeResource
                }
    }

    void testGetSubResourcesAssayMultiMap() {
        setupData()
        Map<HighDimensionDataTypeResource, Long> res = highDimensionResourceService.
                getSubResourcesAssayMultiMap([allPatientsPatientSetConstraint])

        expect:
        res.size() == 2
        res allOf(
                hasEntry(
                        hasProperty('dataTypeName', is('mrna')),
                        containsInAnyOrder(
                                testData.mrnaAssays.collect {
                                    hasSameInterfaceProperties(Assay, it)
                                }
                        )),
                hasEntry(
                        hasProperty('dataTypeName', is(TEST_DATA_TYPE)),
                        containsInAnyOrder(
                                testData.foobarAssays.collect {
                                    hasSameInterfaceProperties(Assay, it)
                                }
                        )))
    }

    void testUnmappedPlatform() {
        setupData()
        def p = new DeGplInfo(
                markerType: 'bogus marker type',
        )
        p.id = 'bogus-platform'
        save([p])

        List<DeSubjectSampleMapping> assays =
                HighDimTestData.createTestAssays(testData.patientsFoobar, -7000, p,
                        HighDimensionResourceServiceTestData.TRIAL_NAME)
        save(assays)

        Map<HighDimensionDataTypeResource, Long> res = highDimensionResourceService.
                getSubResourcesAssayMultiMap([allPatientsPatientSetConstraint])

        expect:
        that(res.values().inject([], { accum, cur -> accum + cur }),
                not(
                        anyOf(
                                assays.collect { Assay it ->
                                    hasItem(
                                            hasSameInterfaceProperties(Assay, it))
                                }
                        )))
    }

    void testAssaysWithMissingPlatform() {
        setupData()
        List<DeSubjectSampleMapping> assays =
                HighDimTestData.createTestAssays(testData.patientsFoobar, -7000, null,
                        HighDimensionResourceServiceTestData.TRIAL_NAME)
        save(assays)

        Map<HighDimensionDataTypeResource, Long> res = highDimensionResourceService.
                getSubResourcesAssayMultiMap([allPatientsPatientSetConstraint])

        expect:
        that(res.values().inject([], { accum, cur -> accum + cur }),
                not(
                        anyOf(
                                assays.collect { Assay it ->
                                    hasItem(
                                            hasSameInterfaceProperties(Assay, it))
                                }
                        )))
    }

    void testWithMultipleConstraints() {
        setupData()
        def trialNameConstraint = highDimensionResourceService.createAssayConstraint(
                name: HighDimensionResourceServiceTestData.MRNA_TRIAL_NAME,
                AssayConstraint.TRIAL_NAME_CONSTRAINT)

        Map<HighDimensionDataTypeResource, Long> res = highDimensionResourceService.
                getSubResourcesAssayMultiMap([
                        allPatientsPatientSetConstraint,
                        trialNameConstraint])

        expect:
        res.size() == 1
        res hasEntry(
                hasProperty('dataTypeName', is('mrna')),
                containsInAnyOrder(
                        testData.mrnaAssays.collect {
                            hasSameInterfaceProperties(Assay, it)
                        }
                ))
    }

    void testBogusConstraint() {
        setupData()

        when:
        highDimensionResourceService.createAssayConstraint([:],
                'bogus constraint name')
        then:
        thrown(InvalidArgumentsException)
    }

    void testInvalidParametersConstraint() {
        setupData()

        when:
        highDimensionResourceService.createAssayConstraint(
                foobar: [],
                AssayConstraint.PATIENT_SET_CONSTRAINT)
        then:
        thrown(InvalidArgumentsException)
    }

    void testEqualityOfReturnedHighDimensionDataTypeResources() {
        setupData()
        def instance1 = highDimensionResourceService.getSubResourceForType('mrna')
        def instance2 = highDimensionResourceService.getSubResourceForType('mrna')

        expect:
        instance1 is(equalTo(instance2))
    }

    void testUnEqualityOfReturnedHighDimensionDataTypeResources() {
        setupData()
        def instance1 = highDimensionResourceService.getSubResourceForType('mrna')
        def instance2 = highDimensionResourceService.getSubResourceForType('vcf')

        expect:
        instance1 is(not(equalTo(instance2)))
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

    @Lazy
    QtQueryMaster allPatientsQueryMaster = createQueryResult('test', patientsBoth + patientsFoobar)

    QueryResult getAllPatientsQueryResult() {
        getQueryResultFromMaster allPatientsQueryMaster
    }

    void saveAll() {
        save([platformMrna, platformFoobar])
        save(patientsBoth + patientsFoobar)
        save(mrnaAssays + foobarAssays)
        save([allPatientsQueryMaster])
    }

}
