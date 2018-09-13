package org.transmartproject.rest.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import org.transmartproject.core.multidimquery.crosstable.CrossTable
import spock.lang.Specification

class CrossTableJsonSerializerSpec extends Specification {

    void 'test cross table serialisation'() {
        given:
        List<List<Long>> rows = [
                [10L, 32L, 332L, 8L, 999L, 29L, 12L, 33L],
                [1L,  123L,  333L, 0L,   92L,   0L,  0L,  3L],
                [11L, 41L,   93L,  937L, 1234L, 0L,  1L,  3L],
                [0L,  3L,    2L,   1L,   1L,    0L,  7L,  0L],
                [73L, 2389L, 212L, 938L, 76L,   92L, 93L, 2L]
        ]
        def table = new CrossTable(rows)

        when:
        def out = new ObjectMapper().writeValueAsString(table)

        then:
        out == '{"rows":' +
                '[[10,32,332,8,999,29,12,33],' +
                '[1,123,333,0,92,0,0,3],' +
                '[11,41,93,937,1234,0,1,3],' +
                '[0,3,2,1,1,0,7,0],' +
                '[73,2389,212,938,76,92,93,2]]}'
    }
}
