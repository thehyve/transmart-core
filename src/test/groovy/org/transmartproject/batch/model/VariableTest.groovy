package org.transmartproject.batch.model

import org.junit.Assert
import org.junit.Test

/**
 *
 */
class VariableTest {

    def filename = 'E-GEOD-8581.sdrf-rewrite.txt'
    def category = 'Subjects'
    def column = 1
    def dataLabel = 'SUBJ_ID'

    @Test
    void testParseCompleteLine() {

        String line = "$filename\t$category\t$column\t$dataLabel"
        Variable map = Variable.LINE_MAPPER.apply(line)

        assertCommonFields(map)
    }

    @Test
    void testParseIncompleteLine() {

        String line = "$filename\t$category\t$column\t$dataLabel"
        Variable map = Variable.LINE_MAPPER.apply(line)

        assertCommonFields(map)
    }

    private void assertCommonFields(Variable map) {
        Assert.assertEquals(filename, map.filename)
        Assert.assertEquals(category, map.categoryCode)
        Assert.assertEquals(column, map.columnNumber + 1) //column number is 1-based in file and 0-based in bean
        Assert.assertEquals(dataLabel, map.dataLabel)
    }

    @Test
    void testParseStream() {
        InputStream input = new FileInputStream('src/test/resources/clinical/E-GEOD-8581_columns.txt')
        List<Variable> list = Variable.parse(input, null)

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
