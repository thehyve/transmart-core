import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.contrib.jackson.JacksonJsonFormatter
import grails.util.BuildSettings
import grails.util.Environment
import org.transmartproject.rest.logging.ApiAuditLogJsonLayout
import org.transmartproject.rest.logging.ChildProcessAppender

// See http://logback.qos.ch/manual/groovy.html for details on configuration
appender('stdout', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%level %logger - %msg%n"
    }
}

root(WARN, ['stdout'])

boolean productionMode = Environment.current == Environment.PRODUCTION
def logDirectory = BuildSettings.TARGET_DIR
if (productionMode) {
    def catalinaBase = System.getProperty('catalina.base') ?: '.'
    logDirectory = "${catalinaBase}/logs".toString()
}
if ((productionMode || Environment.isDevelopmentMode()) && logDirectory) {
    appender("FULL_STACKTRACE", FileAppender) {
        file = "${logDirectory}/stacktrace.log"
        append = true
        encoder(PatternLayoutEncoder) {
            pattern = "%level %logger - %msg%n"
        }
    }
    logger("StackTrace", ERROR, ['FULL_STACKTRACE'], false)
}
if (productionMode && logDirectory) {
    appender('transmart', RollingFileAppender) {
        file = "${logDirectory}/transmart.log"
        encoder(PatternLayoutEncoder) {
            pattern = '%d{dd-MM-yyyy HH:mm:ss,SSS} %5p %c{1} - %m%n'
        }
        triggeringPolicy(SizeBasedTriggeringPolicy) {
            maxFileSize = '100MB'
        }
        rollingPolicy(FixedWindowRollingPolicy) {
            fileNamePattern = "${logDirectory}/transmart.%i.log"
            minIndex = 1
            maxIndex = 9
        }
    }
    root(INFO, ['transmart'])
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

if (Environment.current in [Environment.DEVELOPMENT, Environment.TEST]) {
    logger('org.grails.spring', WARN)
    logger('org.grails.plugins.domain.support.GrailsDomainClassCleaner', WARN)
    logger('grails.plugins.DefaultGrailsPluginManager', WARN) //info to show plugin versions
    logger('org.grails.orm.hibernate.cfg', WARN) //info to show joined-subclass indo
    
    root(INFO, ['stdout'])
}
