package org.transmartproject.rest.serialization

import org.transmartproject.core.multidimquery.CrossTableRepresentation
import org.transmartproject.core.multidimquery.CrossTableRowRepresentation
import spock.lang.Specification

class CrossTableJsonSerializerSpec extends Specification {

    void 'test cross table serialisation'() {
        given:
        def rows = [
                new CrossTableRowRepresentation([10, 32, 332, 8, 999, 29, 12, 33]),
                new CrossTableRowRepresentation([1,  123,  333, 0,   92,   0,  0,  3 ]),
                new CrossTableRowRepresentation([11, 41,   93,  937, 1234, 0,  1,  3 ]),
                new CrossTableRowRepresentation([0,  3,    2,   1,   1,    0,  7,  0 ]),
                new CrossTableRowRepresentation([73, 2389, 212, 938, 76,   92, 93, 2 ])
        ]

        def table = new CrossTableRepresentation(rows)

        when:
        def out = new ByteArrayOutputStream()
        new CrossTableSerializer().write(table.rows, out)

        then:
        out.toString() == '{"rows":' +
                '[[10,32,332,8,999,29,12,33],' +
                '[1,123,333,0,92,0,0,3],' +
                '[11,41,93,937,1234,0,1,3],' +
                '[0,3,2,1,1,0,7,0],' +
                '[73,2389,212,938,76,92,93,2]]}'
    }
}
