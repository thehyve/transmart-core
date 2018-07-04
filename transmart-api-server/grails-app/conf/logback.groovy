import ch.qos.logback.contrib.jackson.JacksonJsonFormatter
import grails.util.BuildSettings
import grails.util.Environment
import org.springframework.boot.logging.logback.ColorConverter
import org.springframework.boot.logging.logback.WhitespaceThrowableProxyConverter
import org.transmartproject.rest.logging.ChildProcessAppender
import org.transmartproject.rest.logging.ApiAuditLogJsonLayout

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

def targetDir = BuildSettings.TARGET_DIR
if (Environment.isDevelopmentMode() && targetDir != null) {
    appender("FULL_STACKTRACE", FileAppender) {
        file = "${targetDir}/stacktrace.log"
        append = true
        encoder(PatternLayoutEncoder) {
            pattern = "%level %logger - %msg%n"
        }
    }
    logger("StackTrace", ERROR, ['FULL_STACKTRACE'], false)
    root(INFO, ['STDOUT', 'FULL_STACKTRACE'])
}
else {
    root(INFO, ['STDOUT'])
}

boolean productionMode = Environment.current == Environment.PRODUCTION
def logDirectory = BuildSettings.TARGET_DIR
if (productionMode) {
    def catalinaBase = System.getProperty('catalina.base') ?: '.'
    logDirectory = "${catalinaBase}/logs".toString()
}

/**
 * Configuration for writing audit metrics.
 * This could be placed in the out-of-tree Config.groovy and will override the configuration below.
 * See https://logback.qos.ch/manual/appenders.html for details on configuration
 */
appender('fileAuditLogger', RollingFileAppender) {
    file = "${logDirectory}/audit.log"
    rollingPolicy(SizeAndTimeBasedRollingPolicy) {
        // daily rollover
        fileNamePattern = "${logDirectory}/audit.%d{yyyy-MM-dd}.%i.log"
        // size limit on the log files
        maxFileSize = '100MB'
        /* // optional parameters
        // maxHistory controls the maximum number of archive files to keep, asynchronously deleting older files.
        maxHistory = 30
        // totalSizeCap controls the total size of all archive files.
        // Oldest archives are deleted asynchronously when the total size cap is exceeded
        totalSizeCap = '3GB'
         */
    }
    encoder(LayoutWrappingEncoder) {
        layout(ApiAuditLogJsonLayout) {
            jsonFormatter(JacksonJsonFormatter) {
                prettyPrint = true
            }
            appendLineSeparator = true
        }
    }
}

appender('processAuditLogger', ChildProcessAppender) {
    // specify the command as in the example below
    //    command = ['/usr/bin/your/command/here', 'arg1', 'arg2']
}

logger('org.transmartproject.db.log', TRACE, ['fileAuditLogger'], true)
logger('org.transmartproject.db.log', TRACE, ['processAuditLogger'], true)
