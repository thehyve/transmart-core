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
import groovy.util.logging.Slf4j
import spock.lang.Specification

import org.gmock.WithGMock

import static org.hamcrest.Matchers.containsInAnyOrder

@WithGMock
@Integration
@Rollback
@Slf4j
class HighDimensionResourceSpec extends Specification {

    /*
     * Calling this HighDimensionResourceServiceTests auto-magically
     * makes this @TesFor(HighDimensionResourceService), which won't do.
     * HighDimensionResourceService has an autowired dependency that cannot
     * be satisfied (StandardAssayConstraintFactory). I first tried to
     * annotate this simply with @TestMixin(ServiceUnitTestMixin) and define
     * this dependent bean in setUp() with defineBeans {}, but that won't do
     * as well because StandardAssayConstraintFactory has, in its turn, other
     * dependencies that cannot be satisfied. At this point, I would have to
     * define those beans as well, which would mean this test would depend
     * on implementation details of collaborators of
     * HighDimensionResourceService, which is not an acceptable situation.
     *
     * So I renamed the test, removed @TestMixin and created the testee
     * manually.
     */

    HighDimensionResourceService testee = new HighDimensionResourceService()

    void testKnownTypes() {
        def dataTypes = [ 'datatype_1', 'datatype_2' ]
        dataTypes.each {
            testee.registerHighDimensionDataTypeModule(it, mock(Closure))
        }

        expect: testee.getKnownTypes() containsInAnyOrder(*dataTypes)
    }
}
