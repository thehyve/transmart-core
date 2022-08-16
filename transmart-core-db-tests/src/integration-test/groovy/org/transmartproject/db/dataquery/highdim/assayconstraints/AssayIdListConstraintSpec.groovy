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

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.db.dataquery.highdim.AssayQuery
import org.transmartproject.db.dataquery.highdim.AssayTestData
import spock.lang.Specification

import static org.hamcrest.Matchers.containsInAnyOrder
import static org.transmartproject.db.test.Matchers.hasSameInterfaceProperties

@Integration
@Rollback
class AssayIdListConstraintSpec extends Specification {

    AssayTestData testData = new AssayTestData()

    void setupData() {
        testData.saveAll()
    }

    void testBasic() {
        setupData()
        def wantedAssays = testData.assays.findAll {
            it.id == -201 || it.id == -301
        }

        def result = new AssayQuery([
                new AssayIdListCriteriaConstraint(
                        ids: wantedAssays*.id
                )
        ]).list()

        expect:
        result containsInAnyOrder(
                wantedAssays.collect {
                    hasSameInterfaceProperties(Assay, it)
                })
    }

}
