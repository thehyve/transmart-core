import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy
import grails.util.BuildSettings
import grails.util.Environment

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
    root(WARN, ['transmart'])
}

/**
 * Configuration for writing audit metrics.
 * This needs to be placed in the out-of-tree Config.groovy, as the log4j config there will override this.
 * (and don't forget to 'import org.apache.log4j.DailyRollingFileAppender',
 * 'import org.transmart.logging.ChildProcessAppender' and 'import org.transmart.logging.JsonLayout'.)
 */
/*

// default log directory is either the tomcat root directory or the
// current working directory.
def catalinaBase = System.getProperty('catalina.base') ?: '.'
def logDirectory = "${catalinaBase}/logs".toString()

// Use layout: JsonLayout(conversionPattern: '%m%n', singleLine: true) to get each message as a single line
// json the same way as ChildProcessAppender sends it.
appender('fileAuditLogger', DailyRollingFileAppender) {
    datePattern = "'.'yyyy-MM-dd",
    fileName = "${logDirectory}/audit.log",
    layout = JsonLayout(conversionPattern:'%d %m%n')
}
// the default layout is a JsonLayout(conversionPattern: '%m%n, singleLine: true)
appender('processAuditLogger', ChildProcessAppender) {
        command = ['/usr/bin/your/command/here', 'arg1', 'arg2']
}

logger('org.transmart.audit', TRACE, ['fileAuditLogger'])
logger('org.transmart.audit', TRACE, ['processAuditLogger'])
logger('org.transmart.audit', TRACE, ['stdout'])
*/

if (Environment.current in [Environment.DEVELOPMENT, Environment.TEST]) {
    logger('org.grails.spring', WARN)
    logger('org.grails.plugins.domain.support.GrailsDomainClassCleaner', WARN)
    logger('grails.plugins.DefaultGrailsPluginManager', WARN) //info to show plugin versions
    logger('org.grails.orm.hibernate.cfg', WARN) //info to show joined-subclass indo

    root(INFO, ['stdout'])
}
