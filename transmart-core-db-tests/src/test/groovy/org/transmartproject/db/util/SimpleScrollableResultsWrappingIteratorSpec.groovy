package org.transmartproject.db.util

import org.hibernate.ScrollableResults
import org.slf4j.Logger
import spock.lang.Specification

import static org.hamcrest.Matchers.*

class SimpleScrollableResultsWrappingIteratorSpec extends Specification {

    def "log error if not closed properly"() {
        ScrollableResultsIterator testee = new ScrollableResultsWrappingIterable<String>(Mock(ScrollableResults))
                .iterator()
        testee.logger = Mock(Logger)

        when:
        testee.finalize()

        then:
        1 * testee.logger.error('Failed to call close before the object was scheduled to be garbage collected')
    }

    def "should fail on getting iterator twice"() {
        def testee = new ScrollableResultsWrappingIterable<String>(Mock(ScrollableResults))

        when:
        testee.iterator()
        testee.iterator()

        then:
        thrown(IllegalStateException)
    }

}
