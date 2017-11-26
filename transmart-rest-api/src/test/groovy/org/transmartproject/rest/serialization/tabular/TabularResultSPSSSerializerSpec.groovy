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

package org.transmartproject.rest.serialization.tabular

import org.transmartproject.core.dataquery.Measure
import org.transmartproject.core.dataquery.MetadataAwareDataColumn
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.VariableDataType
import org.transmartproject.core.dataquery.VariableMetadata
import org.transmartproject.core.users.User
import spock.lang.Specification

import java.util.zip.ZipOutputStream

import static org.transmartproject.rest.serialization.tabular.TabularResultSPSSSerializer.writeSavFile
import static org.transmartproject.rest.serialization.tabular.TabularResultSPSSSerializer.writeSpsFile

class TabularResultSPSSSerializerSpec extends Specification {

    def 'responses on empty table'() {
        def table = Mock(TabularResult)
        table.indicesList >> []

        when: 'producing sps syntax file for empty table'
        writeSpsFile(table, Mock(OutputStream), 'data.tsv')
        then: 'exception is thrown'
        def e1 = thrown(IllegalArgumentException)
        e1.message == "Can't write sps expression file for empty table."

        when: 'producing sav file for empty table'
        writeSavFile(Mock(User), table, Mock(ZipOutputStream))
        then: 'exception is thrown'
        def e2 = thrown(IllegalArgumentException)
        e2.message == "Can't write sav file for empty table."

        when: 'producing spss files for empty table'
        new TabularResultSPSSSerializer().writeFilesToZip(Mock(User), table, Mock(ZipOutputStream))
        then: 'exception is thrown'
        def e3 = thrown(IllegalArgumentException)
        e3.message == "Can't write spss files for empty table."
    }

    def 'responses on table with columns that don\'t have metadata'() {
        def table = Mock(TabularResult)
        def metadatalessColumn = Mock(MetadataAwareDataColumn)
        metadatalessColumn.metadata >> null
        table.indicesList >> [metadatalessColumn]

        when: 'producing sps syntax file for such table'
        writeSpsFile(table, Mock(OutputStream), 'data.tsv')
        then: 'exception is thrown'
        def e1 = thrown(IllegalArgumentException)
        e1.message == "All table columns have to contain metadata."
    }

    def 'responses on table with columns that don\'t have data type metadata'() {
        def table = Mock(TabularResult)
        def typelessColumn = Mock(MetadataAwareDataColumn)
        typelessColumn.metadata >> new VariableMetadata(type: null)
        table.indicesList >> [typelessColumn]

        when: 'producing sps syntax file for such table'
        writeSpsFile(table, Mock(OutputStream), 'data.tsv')
        then: 'exception is thrown'
        def e = thrown(IllegalArgumentException)
        e.message == "Variable has to have a type specified."
    }

    def 'minimal viable sps syntax content'() {
        def table = Mock(TabularResult)
        def column1 = Mock(MetadataAwareDataColumn)
        column1.label >> 'column1'
        column1.metadata >> new VariableMetadata(
                type: VariableDataType.NUMERIC,
        )
        table.indicesList >> [column1]

        when:
        def out = new ByteArrayOutputStream()
        writeSpsFile(table, out, 'data.tsv')
        then:
        def commands = parseSpsCommands(out)
        commands.size() == 2
        commands.first().startsWith('GET DATA ')
        def attributes = commands[0].split('/')*.trim()
        'FILE = "data.tsv"' in attributes
        'VARIABLES = column1 F' in attributes
        commands.last() == 'EXECUTE'
    }

    def 'write spss file content'() {
        def table = Mock(TabularResult)
        def column1 = Mock(MetadataAwareDataColumn)
        column1.label >> 'column1'
        column1.metadata >> new VariableMetadata(
                type: VariableDataType.NUMERIC,
                width: 15,
                measure:  Measure.SCALE,
                decimals: 3,
                description: 'numeric variable',
                valueLabels: [1: 'val1', 2: 'val2'],
        )
        def column2 = Mock(MetadataAwareDataColumn)
        column2.label >> 'column2'
        column2.metadata >> new VariableMetadata(
                type: VariableDataType.DATE,
                width: 20,
                measure:  Measure.ORDINAL,
                description: 'date variable',
        )
        def column3 = Mock(MetadataAwareDataColumn)
        column3.label >> 'column3'
        column3.metadata >> new VariableMetadata(
                type: VariableDataType.STRING,
                width: 30,
                columns: 40,
                measure: Measure.NOMINAL,
                description: 'string variable',
        )
        table.indicesList >> [column1, column2, column3]

        when:
        def out = new ByteArrayOutputStream()
        writeSpsFile(table, out, 'data.tsv')
        then:
        def commands = parseSpsCommands(out)
        commands.size() == 4

        commands.first().startsWith('GET DATA ')
        def getDataAttributes = commands[0].split('/')*.trim()
        'VARIABLES = column1 F15.3 column2 DATETIME20 column3 A30' in getDataAttributes

        def varLabelsCommand = commands.find { it.startsWith('VARIABLE LABELS') }
        varLabelsCommand
        def varLabels = (varLabelsCommand - 'VARIABLE LABELS ').split('/')*.trim()
        varLabels.size() == 3
        'column1 \'numeric variable\'' in varLabels
        'column2 \'date variable\'' in varLabels
        'column3 \'string variable\'' in varLabels

        def valLabelsCommand = commands.find { it.startsWith('VALUE LABELS') }
        valLabelsCommand
        def valLabels = (valLabelsCommand - 'VALUE LABELS ').split('/')*.trim()
        valLabels.size() == 1
        'column1 \'1\' \'val1\' \'2\' \'val2\'' in valLabels

        commands.last() == 'EXECUTE'
    }

    static List<String> parseSpsCommands(ByteArrayOutputStream out) {
        parseSpsCommands(getTextContent(out))
    }

    static List<String> parseSpsCommands(String contentString) {
        contentString.split('(?m)\\.\\s*$')*.replaceAll('\n+', ' ')*.trim().findAll { !it.startsWith('*')}
    }

    static String getTextContent(ByteArrayOutputStream out) {
        new String(out.toByteArray(), 'UTF-8')
    }
}
