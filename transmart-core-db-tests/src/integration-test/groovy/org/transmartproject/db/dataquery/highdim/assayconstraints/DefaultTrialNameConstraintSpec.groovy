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

package org.transmartproject.db.dataquery.highdim.assayconstraints

import grails.test.mixin.TestMixin
import grails.test.mixin.integration.Integration
import grails.test.mixin.web.ControllerUnitTestMixin
import grails.transaction.Rollback
import groovy.util.logging.Slf4j
import spock.lang.Specification

import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.db.dataquery.highdim.AssayQuery
import org.transmartproject.db.dataquery.highdim.AssayTestData

import static org.hamcrest.Matchers.*

@TestMixin(ControllerUnitTestMixin)
@Integration
@Rollback
@Slf4j
class DefaultTrialNameConstraintSpec extends Specification {

    AssayTestData testData = new AssayTestData()

    void setupData() {
        testData.saveAll()
    }

    void testBasic() {
        setupData()
        List<Assay> assays = new AssayQuery([
                new DefaultTrialNameCriteriaConstraint(trialName: 'SAMPLE_TRIAL_2')
        ]).list()

        expect: assays allOf(
                everyItem(
                        hasProperty('trialName', equalTo('SAMPLE_TRIAL_2'))
                ),
                containsInAnyOrder(
                        /* see test data */
                        hasProperty('id', equalTo(-401L)),
                        hasProperty('id', equalTo(-402L)),
                        hasProperty('id', equalTo(-403L)),
                )
        )
    }
}
