/**
 * Default logging configuration for the tranSMART API server.
 * This configuration can be overridden by adding
 * <code>-Dlogging.config=/path/to/logback.groovy</code> to
 * the start script.
 *
 * See https://docs.grails.org/latest/guide/conf.html#externalLoggingConfiguration.
 */

import ch.qos.logback.contrib.jackson.JacksonJsonFormatter
import grails.util.BuildSettings
import grails.util.Environment
import org.springframework.boot.logging.logback.ColorConverter
import org.springframework.boot.logging.logback.WhitespaceThrowableProxyConverter
import org.transmartproject.rest.logging.ApiAuditLogJsonLayout
// import org.transmartproject.rest.logging.ChildProcessAppender

import java.nio.charset.Charset

conversionRule 'clr', ColorConverter
conversionRule 'wex', WhitespaceThrowableProxyConverter

// See http://logback.qos.ch/manual/groovy.html for details on configuration
appender('STDOUT', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        charset = Charset.forName('UTF-8')
        pattern =
                '%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} ' + // Date
                        '%clr(%5p) ' + // Log level
                        '%clr(---){faint} %clr([%15.15t]){faint} ' + // Thread
                        '%clr(%-40.40logger{39}){cyan} %clr(:){faint} ' + // Logger
                        '%m%n%wex' // Message
    }
}

root(WARN, ['STDOUT'])

logger('org.transmartproject.db.log', TRACE, ['STDOUT'], false)
logger('org.apache.catalina.webresources.Cache', ERROR, ['STDOUT'], false)
