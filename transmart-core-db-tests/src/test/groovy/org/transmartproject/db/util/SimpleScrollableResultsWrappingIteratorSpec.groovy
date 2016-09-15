package org.transmartproject.db.util

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import groovy.util.logging.Slf4j
import spock.lang.Specification

import org.apache.log4j.Appender
import org.apache.log4j.Level
import org.apache.log4j.LogManager
import org.apache.log4j.spi.LoggingEvent
import org.gmock.WithGMock
import org.hibernate.ScrollableResults
import org.junit.Ignore

import static org.hamcrest.Matchers.*

@WithGMock
@Integration
@Rollback
@Slf4j
class SimpleScrollableResultsWrappingIteratorSpec extends Specification {

    @Ignore //TODO Fix logging
    void testLogIfNotClosedProperly() {
        def logger = LogManager.getLogger(ScrollableResultsIterator)
        def logEvents = []
        logger.addAppender([doAppend: { LoggingEvent event -> logEvents << event }] as Appender)

        new ScrollableResultsWrappingIterable<String>(mock(ScrollableResults)).iterator().finalize()

        expect: logEvents contains(
                hasProperty('level', equalTo(Level.ERROR))
        )
    }

    void testIteratorIsCalledTwice() {
        def testee = new ScrollableResultsWrappingIterable<String>(mock(ScrollableResults))
        testee.iterator()

        shouldFail(IllegalStateException, { ->
            testee.iterator()
        })
    }

}
