package com.transmart.utils

import grails.test.GrailsUnitTestCase
import org.transmartproject.utils.FileUtils

/**
 * Created with IntelliJ IDEA.
 * User: ruslan
 * Date: 21/06/2013
 * Time: 14:30
 * To change this template use File | Settings | File Templates.
 */
//TODO Add more test cases
class FileUtilsTest extends GrailsUnitTestCase {

    def testCsvFileContent = '"title 1", "title 2", "title 3"\n"1", "Aa", "0.1"\n"2", "Bb", "0.2"\n"3", "Cc", "0.3"'
    def testTsvFileContent = '"title 1"\t"title 2"\t"title 3"\n"1"\t"Aa"\t"0.1"\n"2"\t"Bb"\t"0.2"\n"3"\t"Cc"\t"0.3"'

    File testCsvFile
    File testTsvFile

    protected void setUp() {
        super.setUp()
        testCsvFile = File.createTempFile("file_utils_test_csv", '.tmp')
        testCsvFile.withWriter { it << testCsvFileContent }
        testTsvFile = File.createTempFile("file_utils_test_tsv", '.tmp')
        testTsvFile.withWriter { it << testTsvFileContent }
    }

    protected void tearDown() {
        super.tearDown()
        testCsvFile.delete()
    }

    void testParseTable_basic() {
        def actualResult = FileUtils.parseTable([:], testCsvFile)

        assertNotNull actualResult
        assertEquals 3, actualResult.totalCount
        assertNotNull actualResult.result
        assertEquals actualResult.totalCount, actualResult.result.size()
        assertEquals([
                    ['title 1' : '1', 'title 2' : 'Aa', 'title 3' : '0.1'],
                    ['title 1' : '2', 'title 2' : 'Bb', 'title 3' : '0.2'],
                    ['title 1' : '3', 'title 2' : 'Cc', 'title 3' : '0.3']
                ], actualResult.result)
    }
}
