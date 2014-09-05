package org.transmartproject.batch.model

import org.codehaus.groovy.runtime.DefaultGroovyMethods
import org.codehaus.groovy.runtime.StringGroovyMethods
import org.junit.Assert
import org.junit.Test

/**
 *
 */
class ColumnMappingTest {

    def filename = 'E-GEOD-8581.sdrf-rewrite.txt'
    def category = 'Subjects'
    def column = 1
    def dataLabel = 'SUBJ_ID'

    @Test
    void testParseCompleteLine() {

        String line = "$filename\t$category\t$column\t$dataLabel"
        ColumnMapping map = ColumnMapping.forLine(line)

        assertCommonFields(map)
    }

    @Test
    void testParseIncompleteLine() {

        String line = "$filename\t$category\t$column\t$dataLabel"
        ColumnMapping map = ColumnMapping.forLine(line)

        assertCommonFields(map)
    }

    private void assertCommonFields(ColumnMapping map) {
        Assert.assertEquals(filename, map.filename)
        Assert.assertEquals(category, map.categoryCode)
        Assert.assertEquals(column, map.columnNumber)
        Assert.assertEquals(dataLabel, map.dataLabel)
    }

    @Test
    void testParseStream() {
        InputStream input = new FileInputStream('src/test/resources/clinical/E-GEOD-8581_columns.txt')
        List<ColumnMapping> list = ColumnMapping.parse(input)

        Assert.assertEquals(10, list.size())
        assertCommonFields(list[0])

        //no other easy way of checking stream is closed
        boolean streamClosed = false
        try {
            input.read()
        } catch (IOException ex) {
            streamClosed = true
        }
        Assert.assertTrue("An IO Exception on closed stream was expected", streamClosed)
    }

}
