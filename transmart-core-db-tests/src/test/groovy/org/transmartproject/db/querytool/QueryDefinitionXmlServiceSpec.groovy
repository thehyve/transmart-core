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

package org.transmartproject.db.querytool

import grails.test.mixin.TestFor
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.querytool.ConstraintByValue
import org.transmartproject.core.querytool.Item
import org.transmartproject.core.querytool.Panel
import org.transmartproject.core.querytool.QueryDefinition
import org.w3c.dom.Document
import org.xml.sax.InputSource
import spock.lang.Specification

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

import static org.hamcrest.Matchers.*
import static org.transmartproject.core.querytool.ConstraintByValue.Operator.LOWER_OR_EQUAL_TO
import static org.transmartproject.core.querytool.ConstraintByValue.Operator.LOWER_THAN
import static org.transmartproject.core.querytool.ConstraintByValue.ValueType.NUMBER

@TestFor(QueryDefinitionXmlService)
class QueryDefinitionXmlServiceSpec extends Specification {

    private Document xmlStringToDocument(String xmlString) {
        DocumentBuilder builder =
                DocumentBuilderFactory.newInstance().newDocumentBuilder()
        builder.parse(new InputSource(new StringReader(xmlString)))
    }

    void basicTestFromXml() {
        def xml = '''<ns3:query_definition xmlns:ns3="http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/">
  <query_name>i2b2's Query at Tue Mar 26 2013 10:00:35 GMT+0100</query_name>
  <panel>
    <item>
      <item_key>\\\\i2b2_EXPR\\i2b2\\Expression Profiles Data\\Affymetrix HG-U133\\221610_s_at (55620)\\</item_key>
    </item>
  </panel>
</ns3:query_definition>'''
        /*      */

        def definition = service.fromXml(new StringReader(xml))

        expect:
        definition allOf(
                hasProperty('name', startsWith("i2b2's Query at Tue")),
                hasProperty('panels', contains(allOf(
                        hasProperty('invert', is(false)),
                        hasProperty('items', contains(allOf(
                                hasProperty('conceptKey', startsWith(
                                        '\\\\i2b2_EXPR\\i2b2\\Exp')),
                                hasProperty('constraint', nullValue())
                        )))
                )))
        )
    }

    void testConstrainByValueFromXml() {
        def xml = '''<ns3:query_definition xmlns:ns3="http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/">
  <query_name>i2b2's Query at Tue Mar 26 2013 10:00:35 GMT+0100</query_name>
  <panel>
    <invert>1</invert>
    <item>
      <item_key>\\\\i2b2_EXPR\\i2b2\\Expression Profiles Data\\Affymetrix HG-U133\\221610_s_at (55620)\\</item_key>
      <constrain_by_value>
        <value_operator>LT</value_operator>
        <value_constraint>10</value_constraint>
        <value_type>NUMBER</value_type>
      </constrain_by_value>
    </item>
  </panel>
</ns3:query_definition>'''

        def definition = service.fromXml(new StringReader(xml))
        expect:
        definition.panels[0].invert
        definition.panels[0].items[0].constraint.valueType == NUMBER
        definition.panels[0].items[0].constraint.operator == LOWER_THAN
        definition.panels[0].items[0].constraint.constraint == '10'
    }

    void testMultiplePanelsAndItemsFromXml() {
        def xml = '''<ns3:query_definition xmlns:ns3="http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/">
  <query_name>i2b2's Query at Tue Mar 26 2013 10:00:35 GMT+0100</query_name>
  <panel>
    <invert>1</invert>
    <item>
      <item_key>\\\\i2b2_EXPR\\i2b2\\Expression Profiles Data\\Affymetrix HG-U133\\221610_s_at (55620)\\</item_key>
      <constrain_by_value>
        <value_operator>LT</value_operator>
        <value_constraint>10</value_constraint>
        <value_type>NUMBER</value_type>
      </constrain_by_value>
    </item>
    <item>
      <item_key>\\\\foo\\bar\\</item_key>
    </item>
  </panel>
  <panel>
    <item>
      <item_key>\\\\foo\\bar2\\</item_key>
    </item>
  </panel>
</ns3:query_definition>'''

        def definition = service.fromXml(new StringReader(xml))
        expect:
        definition.panels.size() == 2
        definition.panels[0].items.size() == 2
        definition.panels[1].items.size() == 1
    }

    void testBadXml() {
        when:
        service.fromXml(new StringReader('<foo></bar>'))
        then:
        def e = thrown(InvalidRequestException)
        e.message.contains("Malformed XML document")
    }

    void testBadConstraint() {
        def xml = '''<ns3:query_definition xmlns:ns3="http://www.i2b2
.org/xsd/cell/crc/psm/querydefinition/1.1/">
  <query_name>i2b2's Query at Tue Mar 26 2013 10:00:35 GMT+0100</query_name>
  <panel>
    <item>
      <item_key>\\\\i2b2_EXPR\\i2b2\\Expression Profiles Data\\Affymetrix HG-U133\\221610_s_at (55620)\\</item_key>
      <constrain_by_value>
        <value_operator>LTFOOBARBOGUS</value_operator>
        <value_constraint>10</value_constraint>
        <value_type>NUMBER</value_type>
      </constrain_by_value>
    </item>
  </panel>
</ns3:query_definition>'''
        when:
        service.fromXml(new StringReader(xml))
        then:
        def e = thrown(InvalidRequestException)
        e.message.contains("Invalid XML query definition constraint")
    }

    void basicTestToXml() {
        def queryName = 'The name of my query definition'
        def conceptKey = '\\\\foo\\bar\\'
        def definition = new QueryDefinition(
                queryName,
                [
                        new Panel(
                                invert: true,
                                items: [
                                        new Item(
                                                conceptKey: conceptKey
                                        )
                                ]
                        )
                ]
        )

        def result = xmlStringToDocument(service.toXml(definition))
        expect:
        result allOf(
                hasXPath('/query_definition/query_name', equalTo(queryName)),
                hasXPath('//panel/invert', equalTo('1')),
                hasXPath('/query_definition/panel/item/item_key', equalTo(conceptKey))
        )
    }

    void testConstraintByValueToXml() {
        def definition = new QueryDefinition([
                new Panel(
                        items: [
                                new Item(
                                        conceptKey: '\\\\foo\\bar\\',
                                        constraint: new ConstraintByValue(
                                                valueType: NUMBER,
                                                operator: LOWER_OR_EQUAL_TO,
                                                constraint: '50'
                                        )
                                )
                        ]
                )
        ])
        def result = xmlStringToDocument(service.toXml(definition))
        expect:
        result allOf(
                hasXPath('/query_definition/query_name', startsWith('tranSMART\'s Query')),
                hasXPath('//panel/invert', not(equalTo('1'))),
                hasXPath('//panel/item/constrain_by_value/value_operator', equalTo('LE')),
                hasXPath('//panel/item/constrain_by_value/value_constraint', equalTo('50')),
                hasXPath('//panel/item/constrain_by_value/value_type', equalTo('NUMBER')),
        )
    }

    void testMultiplePanelsAndItemsToXml() {
        def definition = new QueryDefinition([
                new Panel(
                        items: [
                                new Item(conceptKey: '\\\\foo\\bar1\\')
                        ]
                ),
                new Panel(
                        invert: true,
                        items: [
                                new Item(conceptKey: '\\\\foo\\bar2\\'),
                                new Item(conceptKey: '\\\\foo\\bar3\\'),
                        ]
                )
        ])
        def result = xmlStringToDocument(service.toXml(definition))
        expect:
        result allOf(
                hasXPath('count(//panel)', equalTo('2')),
                hasXPath('count(//panel[1]/item)', equalTo('1')),
                hasXPath('count(//panel[2]/item)', equalTo('2')),
        )
    }
}
