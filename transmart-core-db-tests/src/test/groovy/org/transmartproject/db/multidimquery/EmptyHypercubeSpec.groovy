package org.transmartproject.db.multidimquery

import spock.lang.Specification

class EmptyHypercubeSpec extends Specification {

    void testEqualator() {
        def e = new EmptyHypercube()
        def eq = e.getEqualityTester([])

        when:
        eq(null, null)

        then:
        thrown(IllegalArgumentException)
    }

}
