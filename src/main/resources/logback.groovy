appender("stdout", ConsoleAppender) {
  encoder(PatternLayoutEncoder) {
	pattern = "%d [%thread] [%level] %logger - %msg%n"
  }
}

logger("org.springframework.batch", INFO)
//logger("org.springframework.batch", DEBUG)
//logger("org.springframework.jdbc", DEBUG)
logger("org.springframework.beans", INFO)

// don't show messages about bean overrides
logger("org.springframework.beans.factory.support.DefaultListableBeanFactory", WARN)

logger("org.transmartproject", INFO)
root(WARN, ["stdout"])
