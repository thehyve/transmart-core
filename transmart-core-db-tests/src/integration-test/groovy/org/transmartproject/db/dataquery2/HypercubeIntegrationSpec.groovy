package org.transmartproject.db.dataquery2

import spock.lang.Specification

class HypercubeIntegrationSpec extends Specification {

    void testTest() {
        def it = method(1,1)

        expect:
        it == 2
    }

    int method(a, b) {
        return a+b
    }

}
