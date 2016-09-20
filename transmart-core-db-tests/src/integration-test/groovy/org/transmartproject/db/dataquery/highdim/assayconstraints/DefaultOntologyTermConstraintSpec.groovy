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

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.db.dataquery.highdim.AssayQuery
import org.transmartproject.db.dataquery.highdim.AssayTestData
import spock.lang.Specification

import static org.hamcrest.Matchers.*

@Integration
@Rollback
class DefaultOntologyTermConstraintSpec extends Specification {

    ConceptsResource conceptsResourceService

    AssayTestData testData = new AssayTestData()

    void setupData() {
        testData.saveAll()
    }

    void testBasic() {
        setupData()
        List<Assay> assays = new AssayQuery([
                new DefaultOntologyTermCriteriaConstraint(
                        term: conceptsResourceService.getByKey('\\\\i2b2 main\\foo\\bar')
                )
        ]).list()

        /* We should have gotten the assays in the -200 range.
         * Those in the other ranges are assigned to another concept
         */
        expect:
        assays containsInAnyOrder(
                hasProperty('id', equalTo(-201L)),
                hasProperty('id', equalTo(-202L)),
                hasProperty('id', equalTo(-203L)),
        )
    }

    void testOntologyTermConstraintSupportsDisjunctions() {
        setupData()
        List<Assay> assays = new AssayQuery([
                new DisjunctionAssayCriteriaConstraint(constraints: [
                        new DefaultTrialNameCriteriaConstraint(trialName: 'bad name'),
                        new DefaultOntologyTermCriteriaConstraint(
                                term: conceptsResourceService.getByKey('\\\\i2b2 main\\foo\\bar')
                        )])]).list()

        expect:
        assays hasSize(3) /* see basic test */
    }

}
