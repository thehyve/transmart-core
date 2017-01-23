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
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.querytool.*
import org.w3c.dom.Document
import org.xml.sax.InputSource

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.core.querytool.ConstraintByValue.Operator.LOWER_OR_EQUAL_TO
import static org.transmartproject.core.querytool.ConstraintByValue.Operator.LOWER_THAN
import static org.transmartproject.core.querytool.ConstraintByValue.ValueType.NUMBER

@TestMixin(GrailsUnitTestMixin)
@TestFor(QueryDefinitionXmlService)
class QueryDefinitionXmlServiceTests {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private Document xmlStringToDocument(String xmlString) {
        DocumentBuilder builder =
            DocumentBuilderFactory.newInstance().newDocumentBuilder()
        builder.parse(new InputSource(new StringReader(xmlString)))
    }

    @Test
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

        assertThat definition, allOf(
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

    @Test
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
        assertThat definition.panels[0].invert, is(true)
        assertThat definition.panels[0].items, contains(
                hasProperty('constraint', allOf(
                        hasProperty('valueType', equalTo(NUMBER)),
                        hasProperty('operator', equalTo(LOWER_THAN)),
                        hasProperty('constraint', equalTo('10'))
                ))
        )
    }

    @Test
    void testConstrainByMRNAExpressionFromXml() {
        def xml = '''<ns4:query_definition xmlns:ns4="http://www.i2b2.org/xsd/cell/crc/psm/1.1/">
                       <specificity_scale>0</specificity_scale>
                       <panel>
                         <panel_number>0</panel_number>
                         <invert>0</invert>
                         <total_item_occurrences>1</total_item_occurrences>
                         <item>
                           <item_name>MRNA</item_name>
                           <item_key>\\\\code\\full\\name\\</item_key>
                           <tooltip>\\full\\name\\</tooltip>
                           <hlevel>5</hlevel>
                           <class>ENC</class>
                           <constrain_by_omics_value>
                             <omics_value_operator>BETWEEN</omics_value_operator>
                             <omics_value_constraint>-0.5:0.5</omics_value_constraint>
                             <omics_value_type>Gene Expression</omics_value_type>
                             <omics_property>geneSymbol</omics_property>
                             <omics_selector>TNF</omics_selector>
                             <omics_projection_type>log_intensity</omics_projection_type>
                           </constrain_by_omics_value>
                         </item>
                         <panel_timing>ANY</panel_timing>
                       </panel>
                       <query_timing>ANY</query_timing>
                     </ns4:query_definition>'''

        def definition = service.fromXml(new StringReader(xml))

        assertThat definition.panels[0].items, contains(
                hasProperty('constraintByOmicsValue', allOf(
                        hasProperty('operator', equalTo(ConstraintByOmicsValue.Operator.BETWEEN)),
                        hasProperty('constraint', equalTo('-0.5:0.5')),
                        hasProperty('omicsType', equalTo(ConstraintByOmicsValue.OmicsType.GENE_EXPRESSION)),
                        hasProperty('property', equalTo('geneSymbol')),
                        hasProperty('selector', equalTo('TNF')),
                        hasProperty('projectionType', equalTo(Projection.LOG_INTENSITY_PROJECTION))
                ))
        )
    }

    @Test
    void testConstrainByRNASEQRCNTFromXml() {
        def xml = '''<ns4:query_definition xmlns:ns4="http://www.i2b2.org/xsd/cell/crc/psm/1.1/">
                       <specificity_scale>0</specificity_scale>
                       <panel>
                         <panel_number>0</panel_number>
                         <invert>0</invert>
                         <total_item_occurrences>1</total_item_occurrences>
                         <item>
                           <item_name>RNASEQRCNT</item_name>
                           <item_key>\\\\code\\full\\name\\</item_key>
                           <tooltip>\\full\\name\\</tooltip>
                           <hlevel>5</hlevel>
                           <class>ENC</class>
                           <constrain_by_omics_value>
                             <omics_value_operator>BETWEEN</omics_value_operator>
                             <omics_value_constraint>-0.5:0.5</omics_value_constraint>
                             <omics_value_type>RNASEQ_RCNT</omics_value_type>
                             <omics_selector>TNF</omics_selector>
                             <omics_projection_type>log_normalized_readcount</omics_projection_type>
                           </constrain_by_omics_value>
                         </item>
                         <panel_timing>ANY</panel_timing>
                       </panel>
                       <query_timing>ANY</query_timing>
                     </ns4:query_definition>'''

        def definition = service.fromXml(new StringReader(xml))

        assertThat definition.panels[0].items, contains(
                hasProperty('constraintByOmicsValue', allOf(
                        hasProperty('operator', equalTo(ConstraintByOmicsValue.Operator.BETWEEN)),
                        hasProperty('constraint', equalTo('-0.5:0.5')),
                        hasProperty('omicsType', equalTo(ConstraintByOmicsValue.OmicsType.RNASEQ_RCNT)),
                        hasProperty('selector', equalTo('TNF')),
                        hasProperty('projectionType', equalTo(Projection.LOG_NORMALIZED_READ_COUNT_PROJECTION))
                ))
        )
    }

    @Test
    void testConstrainByProteomicsFromXml() {
        def xml = '''<ns4:query_definition xmlns:ns4="http://www.i2b2.org/xsd/cell/crc/psm/1.1/">
                       <specificity_scale>0</specificity_scale>
                       <panel>
                         <panel_number>0</panel_number>
                         <invert>0</invert>
                         <total_item_occurrences>1</total_item_occurrences>
                         <item>
                           <item_name>Proteomics</item_name>
                           <item_key>\\\\code\\full\\name\\</item_key>
                           <tooltip>\\full\\name\\</tooltip>
                           <hlevel>5</hlevel>
                           <class>ENC</class>
                           <constrain_by_omics_value>
                             <omics_value_operator>BETWEEN</omics_value_operator>
                             <omics_value_constraint>-0.5:0.5</omics_value_constraint>
                             <omics_value_type>PROTEOMICS</omics_value_type>
                             <omics_selector>TNF</omics_selector>
                             <omics_projection_type>log_intensity</omics_projection_type>
                           </constrain_by_omics_value>
                         </item>
                         <panel_timing>ANY</panel_timing>
                       </panel>
                       <query_timing>ANY</query_timing>
                     </ns4:query_definition>'''

        def definition = service.fromXml(new StringReader(xml))

        assertThat definition.panels[0].items, contains(
                hasProperty('constraintByOmicsValue', allOf(
                        hasProperty('operator', equalTo(ConstraintByOmicsValue.Operator.BETWEEN)),
                        hasProperty('constraint', equalTo('-0.5:0.5')),
                        hasProperty('omicsType', equalTo(ConstraintByOmicsValue.OmicsType.PROTEOMICS)),
                        hasProperty('selector', equalTo('TNF')),
                        hasProperty('projectionType', equalTo(Projection.LOG_INTENSITY_PROJECTION))
                ))
        )
    }

    @Test
    void testConstrainByMIRNAQPCRFromXml() {
        def xml = '''<ns4:query_definition xmlns:ns4="http://www.i2b2.org/xsd/cell/crc/psm/1.1/">
                       <specificity_scale>0</specificity_scale>
                       <panel>
                         <panel_number>0</panel_number>
                         <invert>0</invert>
                         <total_item_occurrences>1</total_item_occurrences>
                         <item>
                           <item_name>MIRNAQPCR</item_name>
                           <item_key>\\\\code\\full\\name\\</item_key>
                           <tooltip>\\full\\name\\</tooltip>
                           <hlevel>5</hlevel>
                           <class>ENC</class>
                           <constrain_by_omics_value>
                             <omics_value_operator>BETWEEN</omics_value_operator>
                             <omics_value_constraint>-0.5:0.5</omics_value_constraint>
                             <omics_value_type>MIRNA_QPCR</omics_value_type>
                             <omics_selector>TNF</omics_selector>
                             <omics_projection_type>log_intensity</omics_projection_type>
                           </constrain_by_omics_value>
                         </item>
                         <panel_timing>ANY</panel_timing>
                       </panel>
                       <query_timing>ANY</query_timing>
                     </ns4:query_definition>'''

        def definition = service.fromXml(new StringReader(xml))

        assertThat definition.panels[0].items, contains(
                hasProperty('constraintByOmicsValue', allOf(
                        hasProperty('operator', equalTo(ConstraintByOmicsValue.Operator.BETWEEN)),
                        hasProperty('constraint', equalTo('-0.5:0.5')),
                        hasProperty('omicsType', equalTo(ConstraintByOmicsValue.OmicsType.MIRNA_QPCR)),
                        hasProperty('selector', equalTo('TNF')),
                        hasProperty('projectionType', equalTo(Projection.LOG_INTENSITY_PROJECTION))
                ))
        )
    }

    @Test
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
        assertThat definition.panels, allOf(
                hasSize(2),
                contains(
                        hasProperty('items', hasSize(2)),
                        hasProperty('items', hasSize(1))
                )
        )
    }

    @Test
    void testBadXml() {
        expectedException.expect(InvalidRequestException)
        expectedException.expectMessage(containsString("Malformed XML " +
                "document"))

        service.fromXml(new StringReader('<foo></bar>'))
    }

    @Test
    void testBadConstraint() {
        expectedException.expect(InvalidRequestException)
        expectedException.expectMessage(containsString(
                "Invalid XML query definition constraint"))

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

        service.fromXml(new StringReader(xml))
    }

    @Test
    void testBadMarkerType() {
        expectedException.expect(InvalidRequestException)
        expectedException.expectMessage(containsString("Invalid XML query definition highdimension value constraint"))

        def xml = '''<ns4:query_definition xmlns:ns4="http://www.i2b2.org/xsd/cell/crc/psm/1.1/">
                       <specificity_scale>0</specificity_scale>
                       <panel>
                         <panel_number>0</panel_number>
                         <invert>0</invert>
                         <total_item_occurrences>1</total_item_occurrences>
                         <item>
                           <item_name>MRNA</item_name>
                           <item_key>\\\\code\\full\\name\\</item_key>
                           <tooltip>\\full\\name\\</tooltip>
                           <hlevel>5</hlevel>
                           <class>ENC</class>
                           <constrain_by_omics_value>
                             <omics_value_operator>BETWEEN</omics_value_operator>
                             <omics_value_constraint>-0.5:0.5</omics_value_constraint>
                             <omics_value_type>FOOBARTYPE</omics_value_type>
                             <omics_selector>TNF</omics_selector>
                             <omics_projection_type>LOG_INTENSITY</omics_projection_type>
                           </constrain_by_omics_value>
                         </item>
                         <panel_timing>ANY</panel_timing>
                       </panel>
                       <query_timing>ANY</query_timing>
                     </ns4:query_definition>'''

        service.fromXml(new StringReader(xml))
    }

    @Test
    void testBadProjectionType() {
        expectedException.expect(InvalidRequestException)
        expectedException.expectMessage(containsString("Invalid projection type in highdimension value constraint"))

        def xml = '''<ns4:query_definition xmlns:ns4="http://www.i2b2.org/xsd/cell/crc/psm/1.1/">
                       <specificity_scale>0</specificity_scale>
                       <panel>
                         <panel_number>0</panel_number>
                         <invert>0</invert>
                         <total_item_occurrences>1</total_item_occurrences>
                         <item>
                           <item_name>MRNA</item_name>
                           <item_key>\\\\code\\full\\name\\</item_key>
                           <tooltip>\\full\\name\\</tooltip>
                           <hlevel>5</hlevel>
                           <class>ENC</class>
                           <constrain_by_omics_value>
                             <omics_value_operator>BETWEEN</omics_value_operator>
                             <omics_value_constraint>-0.5:0.5</omics_value_constraint>
                             <omics_value_type>Gene Expression</omics_value_type>
                             <omics_selector>TNF</omics_selector>
                             <omics_projection_type>FOO_BAR_PROJECTION</omics_projection_type>
                           </constrain_by_omics_value>
                         </item>
                         <panel_timing>ANY</panel_timing>
                       </panel>
                       <query_timing>ANY</query_timing>
                     </ns4:query_definition>'''

        service.fromXml(new StringReader(xml))
    }

    @Test
    void testBadOperatorType() {
        expectedException.expect(InvalidRequestException)
        expectedException.expectMessage(containsString("Invalid XML query definition highdimension value constraint"))

        def xml = '''<ns4:query_definition xmlns:ns4="http://www.i2b2.org/xsd/cell/crc/psm/1.1/">
                       <specificity_scale>0</specificity_scale>
                       <panel>
                         <panel_number>0</panel_number>
                         <invert>0</invert>
                         <total_item_occurrences>1</total_item_occurrences>
                         <item>
                           <item_name>MRNA</item_name>
                           <item_key>\\\\code\\full\\name\\</item_key>
                           <tooltip>\\full\\name\\</tooltip>
                           <hlevel>5</hlevel>
                           <class>ENC</class>
                           <constrain_by_omics_value>
                             <omics_value_operator>FOOBAROPERATOR</omics_value_operator>
                             <omics_value_constraint>-0.5:0.5</omics_value_constraint>
                             <omics_value_type>Gene Expression</omics_value_type>
                             <omics_selector>TNF</omics_selector>
                             <omics_projection_type>log_intensity</omics_projection_type>
                           </constrain_by_omics_value>
                         </item>
                         <panel_timing>ANY</panel_timing>
                       </panel>
                       <query_timing>ANY</query_timing>
                     </ns4:query_definition>'''

        service.fromXml(new StringReader(xml))
    }

    @Test
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
        assertThat result, allOf(
                hasXPath('/query_definition/query_name', equalTo(queryName)),
                hasXPath('//panel/invert', equalTo('1')),
                hasXPath('/query_definition/panel/item/item_key', equalTo(conceptKey))
        )
    }

    @Test
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
        assertThat result, allOf(
                hasXPath('/query_definition/query_name', startsWith('tranSMART\'s Query')),
                hasXPath('//panel/invert', not(equalTo('1'))),
                hasXPath('//panel/item/constrain_by_value/value_operator', equalTo('LE')),
                hasXPath('//panel/item/constrain_by_value/value_constraint', equalTo('50')),
                hasXPath('//panel/item/constrain_by_value/value_type', equalTo('NUMBER')),
        )
    }

    @Test
    void testMRNAConstraintToXml() {
        def definition = new QueryDefinition([
                new Panel(
                        items: [
                                new Item(
                                        conceptKey: '\\\\code\\full\\name\\',
                                        constraintByOmicsValue: new ConstraintByOmicsValue(
                                                operator: ConstraintByOmicsValue.Operator.BETWEEN,
                                                constraint: '-1:1',
                                                omicsType: ConstraintByOmicsValue.OmicsType.GENE_EXPRESSION,
                                                selector: 'TNF',
                                                projectionType: Projection.LOG_INTENSITY_PROJECTION
                                        )
                                )
                        ]
                )
        ])
        def result = xmlStringToDocument(service.toXml(definition))
        assertThat result, allOf(
                hasXPath('/query_definition/query_name', startsWith('tranSMART\'s Query')),
                hasXPath('//panel/invert', not(equalTo('1'))),
                hasXPath('//panel/item/constrain_by_omics_value/omics_value_operator', equalTo('BETWEEN')),
                hasXPath('//panel/item/constrain_by_omics_value/omics_value_constraint', equalTo('-1:1')),
                hasXPath('//panel/item/constrain_by_omics_value/omics_value_type', equalTo('Gene Expression')),
                hasXPath('//panel/item/constrain_by_omics_value/omics_selector', equalTo('TNF')),
                hasXPath('//panel/item/constrain_by_omics_value/omics_projection_type', equalTo('log_intensity'))
        )
    }

    @Test
    void testRNASEQConstraintToXml() {
        def definition = new QueryDefinition([
                new Panel(
                        items: [
                                new Item(
                                        conceptKey: '\\\\code\\full\\name\\',
                                        constraintByOmicsValue: new ConstraintByOmicsValue(
                                                operator: ConstraintByOmicsValue.Operator.BETWEEN,
                                                constraint: '-1:1',
                                                omicsType: ConstraintByOmicsValue.OmicsType.RNASEQ_RCNT,
                                                selector: 'TNF',
                                                projectionType: Projection.LOG_NORMALIZED_READ_COUNT_PROJECTION
                                        )
                                )
                        ]
                )
        ])
        def result = xmlStringToDocument(service.toXml(definition))
        assertThat result, allOf(
                hasXPath('/query_definition/query_name', startsWith('tranSMART\'s Query')),
                hasXPath('//panel/invert', not(equalTo('1'))),
                hasXPath('//panel/item/constrain_by_omics_value/omics_value_operator', equalTo('BETWEEN')),
                hasXPath('//panel/item/constrain_by_omics_value/omics_value_constraint', equalTo('-1:1')),
                hasXPath('//panel/item/constrain_by_omics_value/omics_value_type', equalTo('RNASEQ_RCNT')),
                hasXPath('//panel/item/constrain_by_omics_value/omics_selector', equalTo('TNF')),
                hasXPath('//panel/item/constrain_by_omics_value/omics_projection_type', equalTo('log_normalized_readcount'))
        )
    }

    @Test
    void testProteomicsConstraintToXml() {
        def definition = new QueryDefinition([
                new Panel(
                        items: [
                                new Item(
                                        conceptKey: '\\\\code\\full\\name\\',
                                        constraintByOmicsValue: new ConstraintByOmicsValue(
                                                operator: ConstraintByOmicsValue.Operator.BETWEEN,
                                                constraint: '-1:1',
                                                omicsType: ConstraintByOmicsValue.OmicsType.PROTEOMICS,
                                                selector: 'TNF',
                                                projectionType: Projection.LOG_INTENSITY_PROJECTION
                                        )
                                )
                        ]
                )
        ])
        def result = xmlStringToDocument(service.toXml(definition))
        assertThat result, allOf(
                hasXPath('/query_definition/query_name', startsWith('tranSMART\'s Query')),
                hasXPath('//panel/invert', not(equalTo('1'))),
                hasXPath('//panel/item/constrain_by_omics_value/omics_value_operator', equalTo('BETWEEN')),
                hasXPath('//panel/item/constrain_by_omics_value/omics_value_constraint', equalTo('-1:1')),
                hasXPath('//panel/item/constrain_by_omics_value/omics_value_type', equalTo('PROTEOMICS')),
                hasXPath('//panel/item/constrain_by_omics_value/omics_selector', equalTo('TNF')),
                hasXPath('//panel/item/constrain_by_omics_value/omics_projection_type', equalTo('log_intensity'))
        )
    }

    @Test
    void testMIRNAQPCRConstraintToXml() {
        def definition = new QueryDefinition([
                new Panel(
                        items: [
                                new Item(
                                        conceptKey: '\\\\code\\full\\name\\',
                                        constraintByOmicsValue: new ConstraintByOmicsValue(
                                                operator: ConstraintByOmicsValue.Operator.BETWEEN,
                                                constraint: '-1:1',
                                                omicsType: ConstraintByOmicsValue.OmicsType.MIRNA_QPCR,
                                                selector: 'TNF',
                                                projectionType: Projection.LOG_INTENSITY_PROJECTION
                                        )
                                )
                        ]
                )
        ])
        def result = xmlStringToDocument(service.toXml(definition))
        assertThat result, allOf(
                hasXPath('/query_definition/query_name', startsWith('tranSMART\'s Query')),
                hasXPath('//panel/invert', not(equalTo('1'))),
                hasXPath('//panel/item/constrain_by_omics_value/omics_value_operator', equalTo('BETWEEN')),
                hasXPath('//panel/item/constrain_by_omics_value/omics_value_constraint', equalTo('-1:1')),
                hasXPath('//panel/item/constrain_by_omics_value/omics_value_type', equalTo('MIRNA_QPCR')),
                hasXPath('//panel/item/constrain_by_omics_value/omics_selector', equalTo('TNF')),
                hasXPath('//panel/item/constrain_by_omics_value/omics_projection_type', equalTo('log_intensity'))
        )
    }

    @Test
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
        assertThat result, allOf(
                hasXPath('count(//panel)', equalTo('2')),
                hasXPath('count(//panel[1]/item)', equalTo('1')),
                hasXPath('count(//panel[2]/item)', equalTo('2')),
        )
    }


}
