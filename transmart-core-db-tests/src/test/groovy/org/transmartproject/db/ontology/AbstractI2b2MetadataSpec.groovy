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
import groovy.util.logging.Slf4j
import spock.lang.Specification


import static org.hamcrest.Matchers.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@Integration
@Rollback
@Slf4j
class AbstractI2b2MetadataSpec extends Specification {

    private AbstractI2b2Metadata testee;

    void before() {
        this.testee = [getTooltip: { -> }] as AbstractI2b2Metadata
    }

    void testGetMetadata() {
        testee.setMetadataxml(METADATA_XML)

        expect: testee.metadata allOf(
                hasEntry('okToUseValues', false),

                hasEntry(equalTo('unitValues'), allOf(
                        hasEntry('normalUnits', 'mg/dl'),
                        hasEntry('equalUnits', 'mg/dl'))),

                hasEntry(equalTo('seriesMeta'), allOf(
                        hasEntry('value', '1'),
                        hasEntry('unit', 'days'),
                        hasEntry('label', 'Day 1'))),
            )
    }

    private static final String METADATA_XML = '''<?xml version="1.0"?>
<ValueMetadata>
  <Version>3.02</Version>
  <CreationDateTime>04/15/2007 01:23:14</CreationDateTime>
  <TestID>UA-BLD</TestID>
  <TestName>Occult Blood</TestName>
  <DataType>Enum</DataType>
  <CodeType>GRP</CodeType>
  <Loinc>5794-3</Loinc>
  <Flagstouse>HL</Flagstouse>
  <Oktousevalues/>
  <MaxStringLength/>
  <LowofLowValue/>
  <HighofLowValue/>
  <LowofHighValue/>
  <HighofHighValue/>
  <LowofToxicValue/>
  <HighofToxicValue/>
  <EnumValues>
    <Val description="">`NEG</Val>
    <Val description="">&amp;gt;3+</Val>
    <Val description="">&amp;gt;3+++</Val>
    <Val description="">1+</Val>
    <Val description="">1-2+</Val>
    <Val description="">2+</Val>
    <Val description="">2++</Val>
    <Val description="">2-3+</Val>
    <Val description="">3+</Val>
    <Val description="">3+++</Val>
    <Val description="">4+</Val>
    <Val description="NEGATIVE">NEG</Val>
    <Val description="">POSITIVE</Val>
    <Val description="">SL TRACE</Val>
    <Val description="">SL. TRACE</Val>
    <Val description="">SL.TRACE</Val>
    <Val description="">TR</Val>
    <Val description="">TRACE</Val>
    <Val description="">TRACE-1+</Val>
    <Val description="">TWO PLUS</Val>
    <Val description="">CANNOT VOID</Val>
    <Val description="">LARGE</Val>
    <Val description="">MOD</Val>
    <Val description="">MODERATE</Val>
    <Val description="NEGATIVE">N</Val>
    <Val description="">NOT ORDERED</Val>
    <Val description="">ROUTINES NOT PERFORMED</Val>
    <Val description="">SM</Val>
    <Val description="">SMALL</Val>
    <Val description="">U</Val>
    <Val description="">UNABLE TO VOID</Val>
    <Val description="">UNS</Val>
    <Val description="">UNSAT.</Val>
    <Val description="NEGATIVE">NEGATIVE</Val>
    <Val description="">NP</Val>
    <Val description="">TEST NOT PERFORMED</Val>
    <Val description="POSITIVE">POS</Val>
    <Val description="">CAN</Val>
    <Val description="">CREDIT</Val>
    <Val description="">MARKED</Val>
    <Val description="">SLIGHT</Val>
  </EnumValues>
  <CommentsDeterminingExclusion>
    <Com/>
  </CommentsDeterminingExclusion>
  <UnitValues>
    <NormalUnits>mg/dl</NormalUnits>
    <EqualUnits>mg/dl</EqualUnits>
    <ExcludingUnits/>
    <ConvertingUnits>
      <Units/>
      <MultiplyingFactor/>
    </ConvertingUnits>
  </UnitValues>
  <Analysis>
    <Enums/>
    <Counts/>
    <New/>
  </Analysis>
  <SeriesMeta>
      <Value>1</Value>
      <Unit>days</Unit>
      <DisplayName>Day 1</DisplayName>
   </SeriesMeta>
</ValueMetadata>
'''
}
