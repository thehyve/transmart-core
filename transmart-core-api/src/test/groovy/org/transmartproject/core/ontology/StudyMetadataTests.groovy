/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-api.
 *
 * Transmart-core-api is free software: you can redistribute it and/or modify it
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
 * transmart-core-api.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.core.ontology

import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class StudyMetadataTests {

    @Test
    void testBlankJsonString() {
        def jsonText = ''

        StudyMetadata metadata = StudyMetadata.fromJson(jsonText)
        assertThat metadata, nullValue()
    }

    @Test
    void testNullJsonValue() {
        def jsonText = 'null'

        StudyMetadata metadata = StudyMetadata.fromJson(jsonText)
        assertThat metadata, nullValue()
    }

    @Test
    void testInvalidJsonValue() {
        def jsonText = 'not a json at all'

        StudyMetadata metadata = StudyMetadata.fromJson(jsonText)
        assertThat metadata, nullValue()
    }

    @Test
    void testEmptyJson() {
        def jsonText = '{}'

        StudyMetadata metadata = StudyMetadata.fromJson(jsonText)
        assertThat metadata, allOf(
                hasProperty('conceptCodeToVariableMetadata', nullValue()),
        )
    }

    @Test
    void testInvalidVarMeta() {
        def jsonText = """
            {
                "conceptCodeToVariableMetadata": {
                    "test1": {  
                       "name": "test1",
                    },
                    "failed": [1, 2, 3],
                    "test2": {  
                       "name": "test2",
                    }
                }
            }
        """

        StudyMetadata metadata = StudyMetadata.fromJson(jsonText)

        assertThat metadata, hasProperty('conceptCodeToVariableMetadata', allOf(
                hasEntry(equalTo('test1'), hasProperty('name', equalTo('test1'))),
                hasEntry(equalTo('test2'), hasProperty('name', equalTo('test2'))),
        ))
    }

    @Test
    void testAllFieldsParsing() {
        def jsonText = """
            {
                "conceptCodeToVariableMetadata": {
                    "test": {  
                       "name": "test1",
                       "type":"Numeric",
                       "measure":"ordinal",
                       "description":"this is description",
                       "width":"15",
                       "decimals":"3",
                       "columns":"18",
                       "valueLabels":{  
                          "-1.0":"M",
                          "1.0":"A",
                          "2.0":"B"
                       },
                       "missingValues": { 
                            "lower": -100,
                            "upper": -10,
                            "value": -10.5,
                        }
                    }
                }
            }
        """

        StudyMetadata metadata = StudyMetadata.fromJson(jsonText)

        assertThat metadata, hasProperty('conceptCodeToVariableMetadata', hasEntry(equalTo('test'), allOf(
                hasProperty('name', equalTo('test1')),
                hasProperty('type', equalTo(VariableDataType.NUMERIC)),
                hasProperty('measure', equalTo(Measure.ORDINAL)),
                hasProperty('description', equalTo('this is description')),
                hasProperty('width', equalTo(15)),
                hasProperty('decimals', equalTo(3)),
                hasProperty('columns', equalTo(18)),
                hasProperty('valueLabels',
                        equalTo([(new BigDecimal('-1.0')): 'M',
                                 (new BigDecimal('1.0')): 'A',
                                 (new BigDecimal('2.0')): 'B'])),
                hasProperty('missingValues', allOf(
                        hasProperty('lower', equalTo(new BigDecimal('-100'))),
                        hasProperty('upper', equalTo(new BigDecimal('-10'))),
                        hasProperty('values', equalTo([new BigDecimal('-10.5')])),
                )),
        )))
    }

    @Test
    void testQuotedMissingValues() {
        def jsonText = """
            {
                "conceptCodeToVariableMetadata": {
                    "test": {  
                       "missingValues":{"values": ["-2.0", "-3.2"]}
                    }
                }
            }
        """

        StudyMetadata metadata = StudyMetadata.fromJson(jsonText)
        assertThat metadata, hasProperty('conceptCodeToVariableMetadata', hasEntry(equalTo('test'),
                hasProperty('missingValues', allOf(
                    hasProperty('upper', nullValue()),
                    hasProperty('lower', nullValue()),
                    hasProperty('values', equalTo([new BigDecimal('-2.0'), new BigDecimal('-3.2')])))
                )))
    }

}
