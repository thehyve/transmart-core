package org.transmartproject.db.dataquery.highdim.snp_lz;

import org.junit.Test;
import static groovy.test.GroovyAssert.*

class TokenListTests {

    static final String testData = "A B CD  E F GHIJ XHJ I UU TU J L GG Y A J S BB UII GHFJ GH J R E L O P F V B A N S N"
    static final List<String> expectedResult = testData.split(" ")
    static final int expectedSize = expectedResult.size()

    @Test
    public void testTokenise() {
        TokenList tokenList = new TokenList(expectedSize)
        StringReader reader = new StringReader(testData)
        tokenList.parse(reader)
        assert tokenList.size() == expectedResult.size()
        assert tokenList == expectedResult
        assert tokenList[0] == "A"
        assert tokenList[1] == "B"
        assert tokenList[2] == "CD" // token with size > 1
        assert tokenList[3] == "" // empty token
        assert tokenList[4] == "E"
    }

    @Test
    public void testMoreTokensThanExpected() {
        TokenList tokenList = new TokenList(expectedSize-1)
        StringReader reader = new StringReader(testData)
        def e = shouldFail(InputMismatchException) {
            tokenList.parse(reader)
        }
        assert e.message.startsWith("Got more tokens than")
    }

    @Test
    public void testLessTokensThanExpected() {
        TokenList tokenList = new TokenList(expectedSize+1)
        StringReader reader = new StringReader(testData)
        def e = shouldFail(InputMismatchException) {
            tokenList.parse(reader)
        }
        assert e.message.startsWith("Expected")
    }

    /**
     * The assumption is that the empty string should result in an
     * empty list, not in a singleton list containing an empty string.
     */
    @Test
    public void testEmptyInput() {
        TokenList tokenList = new TokenList(0)
        StringReader reader = new StringReader("")
        tokenList.parse(reader)
        assert tokenList.size() == 0
        assert tokenList == []
    }

    @Test
    public void testEmptyTokenAtTheEnd() {
        String input = "A "
        List<String> expected = ["A", ""]
        TokenList tokenList = new TokenList(expected.size())
        StringReader reader = new StringReader(input)
        tokenList.parse(reader)
        assert tokenList.size() == expected.size()
        assert tokenList == expected
    }

}
