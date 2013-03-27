package org.transmartproject.db.querytool

import org.transmartproject.core.querytool.ConstraintByValue
import org.transmartproject.core.querytool.Item
import org.transmartproject.core.querytool.Panel
import org.transmartproject.core.querytool.QueryDefinition

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*
import org.w3c.dom.Document
import org.xml.sax.InputSource

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.core.querytool.ConstraintByValue.Operator.*
import static org.transmartproject.core.querytool.ConstraintByValue.ValueType.*

@TestMixin(GrailsUnitTestMixin)
@TestFor(QueryDefinitionXmlService)
class QueryDefinitionXmlServiceTests {

    private Document xmlStringToDocument(String xmlString) {
        DocumentBuilder builder =
            DocumentBuilderFactory.newInstance().newDocumentBuilder()
        builder.parse(new InputSource(new StringReader(xmlString)))
    }

    @Test
    void basicTest() {
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
        assertThat result, allOf(
                hasXPath('/query_definition/query_name', equalTo(queryName)),
                hasXPath('//panel/invert', equalTo('1')),
                hasXPath('/query_definition/panel/item/item_key', equalTo(conceptKey))
        )
    }

    @Test
    void testConstraintByValue() {
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
        assertThat result, allOf(
                hasXPath('/query_definition/query_name', startsWith('tranSMART\'s Query')),
                hasXPath('//panel/invert', not(equalTo('1'))),
                hasXPath('//panel/item/constrain_by_value/value_operator', equalTo('LE')),
                hasXPath('//panel/item/constrain_by_value/value_constraint', equalTo('50')),
                hasXPath('//panel/item/constrain_by_value/value_type', equalTo('NUMBER')),
        )
    }

    @Test
    void testMultiplePanelsAndItems() {
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
        assertThat result, allOf(
                hasXPath('count(//panel)', equalTo('2')),
                hasXPath('count(//panel[1]/item)', equalTo('1')),
                hasXPath('count(//panel[2]/item)', equalTo('2')),
        )
    }
}
