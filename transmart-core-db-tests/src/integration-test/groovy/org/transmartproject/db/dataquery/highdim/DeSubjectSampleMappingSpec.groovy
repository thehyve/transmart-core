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

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.assay.SampleType
import org.transmartproject.core.dataquery.assay.Timepoint
import org.transmartproject.core.dataquery.assay.TissueType
import org.transmartproject.core.dataquery.highdim.Platform
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that

@Integration
@Rollback
class DeSubjectSampleMappingSpec extends Specification {

    SampleHighDimTestData testData

    void setupData() {
        testData = new SampleHighDimTestData()
        testData.saveAll()
    }

    void testSimpleFetchScalarProperties() {
        setupData()
        def assay = DeSubjectSampleMapping.get(testData.assays[0].id)

        expect:
        that(assay, allOf(
                is(notNullValue()),
                hasProperty('siteId', equalTo('site id #1')),
                hasProperty('conceptCode', equalTo('concept code #1')),
                hasProperty('trialName', equalTo(SampleHighDimTestData.TRIAL_NAME)),
                hasProperty('patientInTrialId', equalTo('SUBJ_ID_1'))
        ))
    }

    void testOnDemandProperties() {
        setupData()
        // test the timepoint, sample and tissue properties
        def assay = DeSubjectSampleMapping.get(testData.assays[0].id)

        expect:
        that(assay, allOf(
                is(notNullValue()),
                hasProperty('timepoint', equalTo(new Timepoint('timepoint ' +
                        'code', 'timepoint name #1'))),
                hasProperty('sampleType', equalTo(new SampleType('sample code',
                        'sample name #1'))),
                hasProperty('tissueType', equalTo(new TissueType('tissue code',
                        'tissue name #1'))),
        ))

    }

    void testReferences() {
        setupData()
        def assay = DeSubjectSampleMapping.get(testData.assays[0].id)

        expect:
        that(assay, allOf(
                is(notNullValue()),
                hasProperty('patient', notNullValue(Patient)),
                hasProperty('platform', notNullValue(Platform))))

    }
}
