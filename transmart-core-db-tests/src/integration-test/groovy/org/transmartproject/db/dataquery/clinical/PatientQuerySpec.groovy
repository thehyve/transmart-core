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

package org.transmartproject.db.dataquery.clinical

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.TestData
import org.transmartproject.db.dataquery.clinical.patientconstraints.PatientSetsConstraint
import org.transmartproject.db.dataquery.clinical.patientconstraints.StudyPatientsConstraint
import org.transmartproject.db.i2b2data.I2b2Data
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.ontology.StudyImpl
import spock.lang.Specification

import static org.hamcrest.Matchers.*

@Integration
@Rollback
class PatientQuerySpec extends Specification {

    TestData testData

    void setupData() {
        TestData.prepareCleanDatabase()

        testData = TestData.createDefault()
        testData.saveAll()
    }

    void testStudyPatientsRetrieval() {
        setupData()
        List<PatientDimension> resultList = new PatientQuery([
                new StudyPatientsConstraint(
                        new StudyImpl(id: I2b2Data.DEFAULT_TRIAL_NAME)
                )
        ]).list()

        expect:
        resultList allOf(
                hasSize(testData.i2b2Data.patients.size()),
                everyItem(isA(PatientDimension))
        )
    }


    void testPatientSetsRetrieval() {
        setupData()
        QueryResult queryResult = testData.clinicalData.queryResult

        List<PatientDimension> resultList = new PatientQuery([
                new PatientSetsConstraint(
                        [queryResult]
                )
        ]).list()

        expect:
        resultList allOf(
                hasSize(queryResult.patients.size()),
                everyItem(isA(PatientDimension))
        )
    }
}
