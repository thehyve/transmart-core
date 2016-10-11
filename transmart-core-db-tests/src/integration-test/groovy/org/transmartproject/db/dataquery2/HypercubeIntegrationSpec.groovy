package org.transmartproject.db.dataquery2

import org.transmartproject.db.TransmartSpecification

class HypercubeIntegrationSpec extends TransmartSpecification {

    void testTest() {
        def it = method(1,1)

        expect:
        it == 2
    }

    int method(a, b) {
        return a+b
    }

}
