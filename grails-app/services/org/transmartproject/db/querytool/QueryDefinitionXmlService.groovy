package org.transmartproject.db.querytool

import groovy.xml.MarkupBuilder
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.querytool.ConstraintByValue
import org.transmartproject.core.querytool.Item
import org.transmartproject.core.querytool.Panel
import org.transmartproject.core.querytool.QueryDefinition

/**
 * Handles conversions of {@link org.transmartproject.core.querytool
 * .QueryDefinition}s to and from XML strings, as they are stored in
 * qt_query_master.
 */
class QueryDefinitionXmlService {

    QueryDefinition fromXml(Reader reader) throws InvalidRequestException {
        def xml
        try {
            xml = new XmlSlurper().parse(reader)
        } catch (exception) {
            throw new InvalidRequestException('Malformed XML document: ' +
                    exception.message, exception)
        }

        def convertItem = { item ->
            def data = [ conceptKey: item.item_key ]
            if (item.constrain_by_value.size()) {
                try {
                    def constrain = item.constrain_by_value
                    data.constraint = new ConstraintByValue(
                            valueType: ConstraintByValue.ValueType.valueOf(
                                    constrain.value_type?.toString()),
                            operator: ConstraintByValue.Operator.forValue(
                                    constrain.value_operator.toString()),
                            constraint: constrain.value_constraint?.toString()
                    )
                } catch (err) {
                    throw new InvalidRequestException(
                            'Invalid XML query definition constraint', err)
                }
            }

            new Item(data)
        }
        def panels = xml.panel.collect { panel ->
            new Panel(
                    invert: panel.invert == '1',
                    items: panel.item.collect(convertItem)
            )
        }

        if (xml.query_name.size()) {
            return new QueryDefinition(xml.query_name.toString(), panels)
        } else {
            return new QueryDefinition(panels)
        }
    }

    String toXml(QueryDefinition definition) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)

        /* this XML document is invalid in quite some respects according to
         * the schema, but:
         * 1) that's a subset of what tranSMART used in its requests to CRC
         * 2) i2b2 accepts these documents (not that it matters a lot at this
         * point, since we're not using i2b2's runtime anymore)
         * 3) the schema does not seem correct in many respects; several
         * elements that are supposed to be optional are actually required.
         *
         * It's possible the schema is only used to generate Java classes
         * using JAXB and that there's never any validation against the schema
         */
        xml.'qd:query_definition'('xmlns:qd': "http://www.i2b2" +
                ".org/xsd/cell/crc/psm/querydefinition/1.1/") {
            query_name definition.name

            definition.panels.each { Panel panelArg ->
                panel {
                    invert panelArg.invert ? '1' : '0'
                    panelArg.items.each { Item itemArg ->
                        item {
                            item_key itemArg.conceptKey

                            if (itemArg.constraint) {
                                constrain_by_value {
                                    value_operator itemArg.constraint.operator.value
                                    value_constraint itemArg.constraint.constraint
                                    value_type itemArg.constraint.valueType.name()
                                }
                            }
                        }
                    }
                }
            }
        }

        writer.toString()
    }
}
