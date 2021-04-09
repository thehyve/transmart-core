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

package org.transmartproject.db.user

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.querytool.Item
import org.transmartproject.core.querytool.Panel
import org.transmartproject.core.querytool.QueryDefinition
import org.transmartproject.core.users.LegacyAuthorisationChecks
import org.transmartproject.db.TestData
import org.transmartproject.db.ontology.AcrossTrialsOntologyTerm
import org.transmartproject.db.ontology.AcrossTrialsTestData
import org.transmartproject.db.ontology.I2b2Secure
import org.transmartproject.db.ontology.ModifierDimensionView
import spock.lang.Specification

import static org.transmartproject.db.ontology.ConceptTestData.createI2b2Secure

/*
 * Use separate class because across trials data clashes with standard
 * AccessLevelTestData.
 */

@Integration
@Rollback
class UserAccessLevelAcrossTrialSpec extends Specification {

    @Autowired
    StudiesResource studiesResource

    @Autowired
    SessionFactory sessionFactory

    @Autowired
    LegacyAuthorisationChecks authorisationChecks

    void testQueryDefinitionAllowAcrossTrialNodes() {
        TestData.clearData()
        def acrossTrialsTestData = AcrossTrialsTestData.createDefault()
        acrossTrialsTestData.saveAll()

        User secondUser = acrossTrialsTestData.accessLevelTestData.users[1]

        /* add entry in I2b2Secure to make sure we're not allowing the thing
         * because of the "public by default" behavior */
        OntologyTerm term = new AcrossTrialsOntologyTerm(
                modifierDimension: ModifierDimensionView.get(
                        acrossTrialsTestData.modifierDimensions[0].path))

        I2b2Secure i2b2x = createI2b2Secure(
                fullName: term.fullName,
                name: term.name,
                secureObjectToken: 'EXP:FOOBAR')
        i2b2x.save(flush: true)

        QueryDefinition definition = new QueryDefinition([
                new Panel(items: [new Item(
                        conceptKey: term.key,
                )]),
        ])

        expect:
        authorisationChecks.canRun(secondUser, definition)
    }


}
