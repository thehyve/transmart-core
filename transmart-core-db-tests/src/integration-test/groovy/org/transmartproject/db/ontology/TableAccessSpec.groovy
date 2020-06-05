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

package org.transmartproject.db.ontology

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.db.TestData
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static org.transmartproject.db.ontology.ConceptTestData.addI2b2
import static org.transmartproject.db.ontology.ConceptTestData.addTableAccess
import static spock.util.matcher.HamcrestSupport.that

@Integration
@Rollback
class TableAccessSpec extends Specification {

    void setupData() {
        TestData.prepareCleanDatabase()

        addI2b2(level: 1, fullName: '\\foo\\xpto\\', name: 'xpto')
        addI2b2(level: 1, fullName: '\\foo\\bar\\', name: 'var',
                cVisualattributes: 'FH')
        addI2b2(level: 1, fullName: '\\foo\\baz\\', name: 'baz',
                cSynonymCd: 'Y')
        addI2b2(level: 2, fullName: '\\foo\\xpto\\barn\\', name: 'barn', cVisualattributes: 'FH')
        addI2b2(level: 2, fullName: '\\foo\\xpto\\bart\\', name: 'bart')
        addI2b2(level: 0, fullName: '\\foo\\', name: 'foo')
        addTableAccess(level: 0, fullName: '\\foo\\', name: 'foo',
                tableCode: 'i2b2 main', tableName: 'i2b2')
        addTableAccess(level: 0, fullName: '\\fooh\\', name: 'fooh',
                tableCode: 'bogus table', tableName: 'bogus')
        addTableAccess(level: 0, fullName: '\\notini2b2\\',
                name: 'notini2b2', tableCode: 'i2b2 2nd code',
                tableName: 'i2b2')
    }

    void testBogusTable() {
        setupData()
        def bogusEntry = TableAccess.findByName('fooh');
        assert bogusEntry != null

        when:
        bogusEntry.children
        then:
        def e = thrown(RuntimeException)
        e.message.contains('table bogus is not mapped')
    }

    void testCategoryNotAlsoInReferredTable() {
        setupData()
        def notInI2b2 = TableAccess.findByName('notini2b2')

        assert notInI2b2 != null

        when:
        notInI2b2.children
        then:
        def e = thrown(RuntimeException)
        e.message.contains('could not find it ' +
                'in class org.transmartproject.db.ontology.' +
                'I2b2\'s table (fullname: \\notini2b2\\)')
    }


    void testGetChildren() {
        setupData()
        def cats = TableAccess.getCategories()
        def catFoo = cats.find { it.name == 'foo' }

        expect:
        catFoo instanceof OntologyTerm
        catFoo.children.size() == 1
        catFoo.children[0].name == 'xpto'

        /* show hidden as well */
        that(catFoo.getChildren(true), allOf(
                hasSize(2),
                contains(
                        hasProperty('name', equalTo('var')),
                        hasProperty('name', equalTo('xpto'))
                )
        ))

        /* show also synonyms */
        that(catFoo.getChildren(true, true), allOf(
                hasSize(3),
                contains( /* ordered by name */
                        hasProperty('name', equalTo('baz')),
                        hasProperty('name', equalTo('var')),
                        hasProperty('name', equalTo('xpto')),
                )
        ))
    }

    void testGetAllDescendants() {
        setupData()
        TableAccess ta = TableAccess.findByName('foo')

        expect:
        that(ta.allDescendants, allOf(
                hasSize(2),
                contains(
                        hasProperty('name', equalTo('bart')),
                        hasProperty('name', equalTo('xpto')),
                )
        ))
        that(ta.getAllDescendants(true, true), allOf(
                hasSize(5),
                contains(
                        hasProperty('name', equalTo('barn')),
                        hasProperty('name', equalTo('bart')),
                        hasProperty('name', equalTo('baz')),
                        hasProperty('name', equalTo('var')),
                        hasProperty('name', equalTo('xpto')),
                )
        ))

    }
}
