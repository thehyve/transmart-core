/*
 * Copyright 2014 Janssen Research & Development, LLC.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version, along with the following terms:
 *
 *   1. You may convey a work based on this program in accordance with
 *      section 5, provided that you retain the above notices.
 *   2. You may convey verbatim copies of this program code as you receive
 *      it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import com.google.common.collect.ImmutableList
import grails.util.Environment
import grails.util.Holders
import groovy.util.logging.Log4j

@Log4j
class BootStrap {

    final static String TEST_PHASE_CONFIGURER_CLASS_NAME =
            'org.codehaus.groovy.grails.test.runner.phase.IntegrationTestPhaseConfigurer'

    def init = { servletContext ->
        if (Environment.currentEnvironment == Environment.TEST) {
            if (Class.forName(TEST_PHASE_CONFIGURER_CLASS_NAME).currentApplicationContext) {
                /* don't load the test data bundle for integration tests */
                return
            }
            def testData = createTestData()
            log.info 'About to save test data'
            testData.saveAll()
            log.info 'Saved test data'

            def queryResults = testData.mrnaData.patients.collect { patient ->
                Class.forName('org.transmartproject.db.querytool.QueryResultData')
                        .getMethod('createQueryResult', List)
                        .invoke(null, [patient])
            }

            // hibernate is not saving QtQueryMaster before QtQueryInstance if
            // the id of QtQueryMaster is explicitly assigned, hence we add
            // an endpoint to retrieve these result instance ids
            // (see SmartRTestController)

            Holders.applicationContext.registerSingleton('mrnaPatientSetIds', ArrayList)
            Holders.applicationContext.getBean('mrnaPatientSetIds').addAll(
                    ImmutableList.copyOf(queryResults*.save()*.id))
            log.info 'Created extra patient sets for testing'
        }
    }

    def createTestData() {
        Class clazz = Class.forName('org.transmartproject.db.TestData')
        clazz.getMethod('createDefault').invoke(null) //static method
    }
}
