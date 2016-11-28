import grails.util.BuildSettings
import grails.util.Environment

// See http://logback.qos.ch/manual/groovy.html for details on configuration
appender('STDOUT', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%level %logger - %msg%n"
    }
}

if (Environment.current in [Environment.TEST, Environment.DEVELOPMENT]) {
    root(INFO, ['STDOUT'])
    logger('org.hibernate.type', TRACE)
} else {
    root(ERROR, ['STDOUT'])
}

logger('org.codehaus.groovy.grails.commons.spring', WARN)
logger('org.codehaus.groovy.grails.domain.GrailsDomainClassCleaner', WARN)

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
