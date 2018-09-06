package org.transmartproject.rest.serialization.tabular

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import org.transmartproject.core.dataquery.DataColumn
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.users.User
import org.transmartproject.db.multidimquery.ConceptDimension
import org.transmartproject.db.multidimquery.HypercubeDataColumn
import org.transmartproject.db.multidimquery.HypercubeDataRow
import org.transmartproject.db.multidimquery.StudyDimension
import spock.lang.Specification

import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

import static org.transmartproject.core.multidimquery.hypercube.Dimension.Density.DENSE
import static org.transmartproject.core.multidimquery.hypercube.Dimension.Packable.NOT_PACKABLE
import static org.transmartproject.core.multidimquery.hypercube.Dimension.Size.MEDIUM
import static org.transmartproject.core.multidimquery.hypercube.Dimension.Size.SMALL

class TabularResultTSVSerializerSpec extends Specification {

    final StudyDimension STUDY =     new StudyDimension(SMALL, DENSE, NOT_PACKABLE)
    final ConceptDimension CONCEPT = new ConceptDimension(MEDIUM, DENSE, NOT_PACKABLE)

    /**
     * Test that the TSV serialiser uses the columns as passed to the constructor instead of the
     * columns of the individual tabular results that are passed to it.
     * If multiple tabular results are passed to the serialiser, e.g., by different concurrent
     * workers, they should use the same columns to produce a consistent result, that can be
     * appended by the combine operation.
     */
    def 'test TSV serialisation for tabular data'() {
        def user = Mock(User)
        user.getUsername() >> { 'test' }
        ByteArrayOutputStream bout = new ByteArrayOutputStream()
        def out = new ZipOutputStream(bout)
        def column1 = new HypercubeDataColumn(ImmutableMap.copyOf([(STUDY): 'studyA', (CONCEPT): 'age']))
        def column2 = new HypercubeDataColumn(ImmutableMap.copyOf([(STUDY): 'studyB', (CONCEPT): 'age']))
        def column3 = new HypercubeDataColumn(ImmutableMap.copyOf([(STUDY): 'studyA', (CONCEPT): 'height']))
        ImmutableList<DataColumn> columns = ImmutableList.builder()
            .add(column1, column2, column3)
            .build()
        def serialiser = new TabularResultTSVSerializer(user, out, columns)
        def expected = [
                ['1', '', '2'],
                ['3', '4', ''],
                ['', '5', '6']
        ]

        when:
        def table1 = Mock(TabularResult)
        def row1 = Mock(HypercubeDataRow)
        row1.getAt(column1) >> 1
        row1.getAt(column3) >> 2
        def row2 = Mock(HypercubeDataRow)
        row2.getAt(column1) >> 3
        row2.getAt(column2) >> 4
        List<DataRow> rows1 = [row1, row2]
        table1.iterator() >> rows1.iterator()
        def table2 = Mock(TabularResult)
        def row3 = Mock(HypercubeDataRow)
        row3.getAt(column2) >> 5
        row3.getAt(column3) >> 6
        List<DataRow> rows2 = [row3]
        table2.iterator() >> rows2.iterator()

        serialiser.writeParallel(table1, 1)
        serialiser.writeParallel(table2, 2)
        serialiser.combine()
        out.flush()

        then:
        def bytes = new ByteArrayInputStream(bout.toByteArray())
        def zipIn = new ZipInputStream(bytes)
        def entry = zipIn.nextEntry
        entry.name == 'data.tsv'
        def tsvReader = new CSVReaderBuilder(new InputStreamReader(zipIn))
                .withCSVParser(new CSVParserBuilder().withSeparator(AbstractTSVSerializer.COLUMN_SEPARATOR).build())
                .build()
        def lines = tsvReader.readAll()
        !lines.isEmpty()
        lines[0] as List<String> == ['study: studyA, concept: age', 'study: studyB, concept: age', 'study: studyA, concept: height']
        lines.eachWithIndex { it, index ->
            println it
            assert it.length == columns.size()
            if (index > 0) {
                assert it as List<String> == expected[index - 1]
            }
        }
    }

}
