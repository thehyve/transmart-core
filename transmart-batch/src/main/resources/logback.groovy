import ch.qos.logback.classic.gaffer.GafferUtil

import java.nio.file.Files
import java.nio.file.Paths

def overrideFile = Paths.get('logback.groovy')

if (Files.isRegularFile(overrideFile)) {
  println "-> Using custom logback configuration fom ${overrideFile.toAbsolutePath()}"
  // The GroovyShell created script this file is parsed into has
  // ConfigurationDelegate mixed in. That's where getContext() comes from
  GafferUtil.runGafferConfiguratorOn(
                  context, null, overrideFile.toFile())
  return
}

/******* CUT HERE *******/

appender("stdout", ConsoleAppender) {
  encoder(PatternLayoutEncoder) {
    pattern = "%d [%thread] [%level] %logger{20} - %msg%n"
  }
}

logger("org.springframework.batch", INFO)
//logger("org.springframework.batch", DEBUG)
//logger("org.springframework.jdbc", DEBUG)
logger("org.springframework.beans", INFO)

// don't show messages about bean overrides
logger("org.springframework.beans.factory.support.DefaultListableBeanFactory", WARN)

logger("org.transmartproject", INFO)

// change to DEBUG to know about successful mappings and unmapped nodes
logger("org.transmartproject.batch.clinical.xtrial.XtrialMappingCollection", INFO)
root(WARN, ["stdout"])
