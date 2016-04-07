package org.transmartproject.batch.test

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory

/**
 * Static methods to help in testing logging
 */
class TestLogUtils {

    static ListAppender<ILoggingEvent> initAndAppendListAppenderToTheRootLogger() {
        def rootLog = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
        ListAppender<ILoggingEvent> listAppender = new ListAppender<ILoggingEvent>()
        listAppender.start()
        rootLog.addAppender(listAppender)

        listAppender
    }

    static removeListAppenderFromTheRootLoggger(ListAppender<ILoggingEvent> listAppender) {
        def rootLog = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
        rootLog.detachAppender(listAppender)
    }

}
