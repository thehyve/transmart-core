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

import com.google.common.collect.ImmutableList
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import org.transmartproject.core.dataquery.DataColumn
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.MetadataAwareDataColumn
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.ValueFetchingDataColumn
import org.transmartproject.core.ontology.Measure
import org.transmartproject.core.ontology.MissingValues
import org.transmartproject.core.ontology.VariableDataType
import org.transmartproject.core.ontology.VariableMetadata
import org.transmartproject.core.users.User
import org.transmartproject.db.multidimquery.HypercubeDataRow
import spock.lang.Specification
import java.util.zip.ZipOutputStream

import static org.transmartproject.rest.serialization.tabular.TabularResultSPSSSerializer.writeSpsFile

class TabularResultSPSSSerializerSpec extends Specification {

    private static interface MetadataAwareValueFetchingDataColumn extends MetadataAwareDataColumn, ValueFetchingDataColumn {}

    private final UTC = TimeZone.getTimeZone('UTC')
    private final CEST = TimeZone.getTimeZone('CEST')

    def 'responses on empty table'() {
        def table = Mock(TabularResult)
        table.indicesList >> []
        def columns = ImmutableList.copyOf([] as List<DataColumn>)

        when: 'producing spss files for empty table'
        new TabularResultSPSSSerializer(Mock(User), Mock(ZipOutputStream), columns, 'testFile')
                .writeParallel(table, 1)
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
        writeSpsFile([metadatalessColumn], Mock(OutputStream), 'data.tsv')
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
        writeSpsFile(table.indicesList, Mock(OutputStream), 'data.tsv', 'data.sps')
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
        writeSpsFile(table.indicesList, out, 'data.tsv', 'data.sps')
        then:
        def commands = parseSpsCommands(out)
        commands.size() == 3
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
                valueLabels: [(new BigDecimal(1)): 'val1', (new BigDecimal(2)): 'val2'],
        )
        def column2 = Mock(MetadataAwareDataColumn)
        column2.label >> 'column2'
        column2.metadata >> new VariableMetadata(
                type: VariableDataType.DATE,
                width: 20,
                columns: 25,
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
        writeSpsFile(table.indicesList, out, 'data.tsv', 'data.sps')
        then:
        def commands = parseSpsCommands(out)
        commands.size() == 7

        commands.first().startsWith('GET DATA ')
        def getDataAttributes = commands[0].split('/')*.trim()
        'VARIABLES = column1 F15.3 column2 DATE20 column3 A30' in getDataAttributes

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

        def varLevelsCommand = commands.find { it.startsWith('VARIABLE LEVEL') }
        varLevelsCommand
        def varLevels = (varLevelsCommand - 'VARIABLE LEVEL ').split('/')*.trim()
        varLevels.size() == 3
        'column1 (SCALE)' in varLevels
        'column2 (ORDINAL)' in varLevels
        'column3 (NOMINAL)' in varLevels

        def varWidthCommand = commands.find { it.startsWith('VARIABLE WIDTH') }
        varWidthCommand
        def varWidths = (varWidthCommand - 'VARIABLE WIDTH ').split('/')*.trim()
        varWidths.size() == 2
        'column2 (25)' in varWidths
        'column3 (40)' in varWidths

        commands.last() == 'EXECUTE'
    }

    def 'quotes escaping'() {
        def table = Mock(TabularResult)
        def column1 = Mock(MetadataAwareDataColumn)
        column1.label >> 'column1'
        column1.metadata >> new VariableMetadata(
                type: VariableDataType.NUMERIC,
                description: 'this variable has \' in the middle',
                valueLabels: [(new BigDecimal(1)): 'val\'1', (new BigDecimal(2)): 'val\'2'],
        )
        table.indicesList >> [column1]

        when:
        def out = new ByteArrayOutputStream()
        writeSpsFile(table.indicesList, out, 'data.tsv', 'data.sps')
        then:
        def commands = parseSpsCommands(out)
        commands.size() == 5

        def varLabelsCommand = commands.find { it.startsWith('VARIABLE LABELS') }
        varLabelsCommand
        def varLabels = (varLabelsCommand - 'VARIABLE LABELS ').split('/')*.trim()
        varLabels.size() == 1
        'column1 \'this variable has \'\' in the middle\'' in varLabels

        def valLabelsCommand = commands.find { it.startsWith('VALUE LABELS') }
        valLabelsCommand
        def valLabels = (valLabelsCommand - 'VALUE LABELS ').split('/')*.trim()
        valLabels.size() == 1
        'column1 \'1\' \'val\'\'1\' \'2\' \'val\'\'2\'' in valLabels
    }

    def 'new lines escaping'() {
        def user = Mock(User)
        user.getUsername() >> { 'test' }
        ByteArrayOutputStream bout = new ByteArrayOutputStream()
        def out = new ZipOutputStream(bout)
        def table = Mock(TabularResult)
        def column1 = Mock(MetadataAwareValueFetchingDataColumn)
        column1.label >> 'column1'
        column1.metadata >> new VariableMetadata(
                type: VariableDataType.STRING,
                width: 30,
                columns: 40,
                measure: Measure.NOMINAL,
                description: 'string variable',
        )
        def columns = ImmutableList.copyOf([column1] as List<DataColumn>)
        table.indicesList >> [column1]
        def row1 = Mock(HypercubeDataRow)
        row1.getAt(column1) >> "this string contains line break\nin the middle"
        List<DataRow> rows = [row1]
        table.rows >> rows.iterator()

        when: 'producing tsv files '
        new TabularResultSPSSSerializer(user, out, columns, 'testFile')
                .writeValues(columns, table, bout)
        def bytes = new ByteArrayInputStream(bout.toByteArray())
        def tsvReader = new CSVReaderBuilder(new InputStreamReader(bytes))
                .withCSVParser(new CSVParserBuilder().withSeparator(AbstractTSVSerializer.COLUMN_SEPARATOR).build())
                .build()
        def lines = tsvReader.readAll()

        then:
        !lines.isEmpty()
        lines.size() == 1
        lines[0][0] == "this string contains line break in the middle"
    }

    def 'missing values'() {
        def table = Mock(TabularResult)
        def column1 = Mock(MetadataAwareDataColumn)
        column1.label >> 'column1'
        column1.metadata >> new VariableMetadata(
                type: VariableDataType.NUMERIC,
                missingValues: new MissingValues(
                        upper: new BigDecimal(-1),
                        lower: new BigDecimal(-10),
                        values: [ new BigDecimal('-12.5') ]
                )
        )
        def column2 = Mock(MetadataAwareDataColumn)
        column2.label >> 'column2'
        column2.metadata >> new VariableMetadata(
                type: VariableDataType.NUMERIC,
                missingValues: new MissingValues(
                        values: [ new BigDecimal('-100'), new BigDecimal('-200'), new BigDecimal('-300') ]
                )
        )
        def column3 = Mock(MetadataAwareDataColumn)
        column3.label >> 'column3'
        column3.metadata >> new VariableMetadata(
                type: VariableDataType.NUMERIC,
                missingValues: new MissingValues(
                        upper: new BigDecimal(-1),
                )
        )
        def column4 = Mock(MetadataAwareDataColumn)
        column4.label >> 'column4'
        column4.metadata >> new VariableMetadata(
                type: VariableDataType.NUMERIC,
                missingValues: new MissingValues(
                        lower: new BigDecimal(100),
                )
        )
        def column5 = Mock(MetadataAwareDataColumn)
        column5.label >> 'column5'
        column5.metadata >> new VariableMetadata(
                type: VariableDataType.STRING,
                missingValues: new MissingValues(
                        values: ['-1'],
                )
        )
        table.indicesList >> [column1, column2, column3, column4, column5]

        when:
        def out = new ByteArrayOutputStream()
        writeSpsFile(table.indicesList, out, 'data.tsv', 'data.sps')
        then:
        def commands = parseSpsCommands(out)
        commands.size() == 4
        def missingValuesCommand = commands.find { it.startsWith('MISSING VALUES') }
        missingValuesCommand
        def missingValuesDeclarations = (missingValuesCommand - 'MISSING VALUES ').split('/')*.trim()
        missingValuesDeclarations.size() == 5
        'column1 (-10 THRU -1, -12.5)' in missingValuesDeclarations
        'column2 (-100, -200, -300)' in missingValuesDeclarations
        'column3 (LOWEST THRU -1)' in missingValuesDeclarations
        'column4 (100 THRU HIGHEST)' in missingValuesDeclarations
        'column5 (\'-1\')' in missingValuesDeclarations
    }

    def 'write tsv file content with date values'() {
        def user = Mock(User)
        user.getUsername() >> { 'test' }
        ByteArrayOutputStream bout = new ByteArrayOutputStream()
        def out = new ZipOutputStream(bout)
        def table = Mock(TabularResult)
        def column1 = Mock(MetadataAwareValueFetchingDataColumn)
        column1.label >> 'column1'
        column1.metadata >> new VariableMetadata(
                type: VariableDataType.DATETIME,
                width: 20,
                columns: 25,
                measure:  Measure.SCALE,
                description: 'datetime variable',
        )
        def column2 =Mock(MetadataAwareValueFetchingDataColumn)
        column2.label >> 'column2'
        column2.metadata >> new VariableMetadata(
                type: VariableDataType.DATE,
                width: 20,
                columns: 25,
                measure:  Measure.SCALE,
                description: 'date variable',
        )
        def column3 = Mock(MetadataAwareValueFetchingDataColumn)
        column3.label >> 'column3'
        column3.metadata >> new VariableMetadata(
                type: VariableDataType.STRING,
                width: 30,
                columns: 40,
                measure: Measure.NOMINAL,
                description: 'string variable',
        )
        def columns = ImmutableList.copyOf([column1, column2, column3] as List<DataColumn>)
        table.indicesList >> [column1, column2, column3]
        def row1 = Mock(HypercubeDataRow)
        row1.getAt(column1) >> Date.parse('yyyy-MM-dd hh:mm:ss', '2001-09-01 09:45:18', UTC)
        row1.getAt(column2) >> Date.parse('yyyy-MM-dd hh:mm:ss', '2009-12-01 09:45:18', UTC)
        row1.getAt(column3) >> Date.parse('yyyy-MM-dd hh:mm:ss', '2001-09-01 09:45:18', UTC)
        def row2 = Mock(HypercubeDataRow)
        row2.getAt(column1) >> Date.parse('dd-MM-yyyy', '28-11-2005', UTC)
        row2.getAt(column2) >> Date.parse('dd-MM-yyyy hh:mm:ss', '12-02-1998 00:45:33', CEST)
        row2.getAt(column3) >> Date.parse('dd-MM-yyyy hh:mm:ss', '12-02-1998 18:30:05', CEST)
        List<DataRow> rows = [row1, row2]
        table.rows >> rows.iterator()

        when: 'producing tsv files '
        new TabularResultSPSSSerializer(user, out, columns, 'testFile')
                .writeValues(columns, table, bout)
        def bytes = new ByteArrayInputStream(bout.toByteArray())
        def tsvReader = new CSVReaderBuilder(new InputStreamReader(bytes))
                .withCSVParser(new CSVParserBuilder().withSeparator(AbstractTSVSerializer.COLUMN_SEPARATOR).build())
                .build()
        def lines = tsvReader.readAll()

        then:
        !lines.isEmpty()
        lines.size() == 2
        lines[0][0] == "01-09-2001 09:45:18"
        lines[0][1] == "01-12-2009"
        lines[0][2] == "01-09-2001 09:45:18"
        lines[1][0] == "28-11-2005 12:00:00"
        lines[1][1] == "12-02-1998"
        lines[1][2] == "12-02-1998 06:30:05"

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
