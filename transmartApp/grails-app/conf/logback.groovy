import ch.qos.logback.classic.encoder.PatternLayoutEncoder
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

if (Environment.current in [Environment.DEVELOPMENT, Environment.TEST]) {
    logger('org.grails.spring', WARN)
    logger('org.grails.plugins.domain.support.GrailsDomainClassCleaner', WARN)
    logger('grails.plugins.DefaultGrailsPluginManager', WARN) //info to show plugin versions
    logger('org.grails.orm.hibernate.cfg', WARN) //info to show joined-subclass indo

    root(INFO, ['stdout'])
}
