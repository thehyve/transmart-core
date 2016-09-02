import grails.util.BuildSettings
import grails.util.Environment

// See http://logback.qos.ch/manual/groovy.html for details on configuration
appender('stdout', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%level %logger - %msg%n"
    }
}

root(ERROR, ['stdout'])

def targetDir = BuildSettings.TARGET_DIR
if (Environment.isDevelopmentMode() && targetDir) {
    appender("FULL_STACKTRACE", FileAppender) {
        file = "${targetDir}/stacktrace.log"
        append = true
        encoder(PatternLayoutEncoder) {
            pattern = "%level %logger - %msg%n"
        }
    }
    logger("StackTrace", ERROR, ['FULL_STACKTRACE'], false)
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

if (Environment.is(Environment.TEST)) {
    logger('org.codehaus.groovy.grails.commons.spring', WARN)
    logger('org.codehaus.groovy.grails.domain.GrailsDomainClassCleaner', WARN)
    logger('org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager', WARN) //info to show plugin versions
    logger('org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainBinder', WARN) //info to show joined-subclass indo

    root(INFO, ['stdout'])
}

logger('org.codehaus.groovy.grails.commons.cfg.ConfigurationHelper', WARN)
