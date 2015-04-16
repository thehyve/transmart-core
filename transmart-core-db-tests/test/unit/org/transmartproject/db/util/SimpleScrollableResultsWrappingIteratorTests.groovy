package org.transmartproject.db.util

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.apache.log4j.Appender
import org.apache.log4j.Level
import org.apache.log4j.LogManager
import org.apache.log4j.spi.LoggingEvent
import org.gmock.WithGMock
import org.hibernate.ScrollableResults
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@TestMixin(GrailsUnitTestMixin)
@WithGMock
class SimpleScrollableResultsWrappingIteratorTests {

    @Test
    void testLogIfNotClosedProperly() {
        def logger = LogManager.getLogger(SimpleScrollableResultsIterator)
        def logEvents = []
        logger.addAppender([doAppend: { LoggingEvent event -> logEvents << event }] as Appender)

        new SimpleScrollableResultsWrappingIterable<String>(mock(ScrollableResults)).iterator().finalize()

        assertThat logEvents, contains(
                hasProperty('level', equalTo(Level.ERROR))
        )
    }

    @Test
    void testIteratorIsCalledTwice() {
        def testee = new SimpleScrollableResultsWrappingIterable<String>(mock(ScrollableResults))
        testee.iterator()

        shouldFail(IllegalStateException, { ->
            testee.iterator()
        })
    }

}
